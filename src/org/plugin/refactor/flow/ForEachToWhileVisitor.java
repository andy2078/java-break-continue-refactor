package org.plugin.refactor.flow;

import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import java.util.Set;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Type;


public class ForEachToWhileVisitor extends ASTVisitor {

    private final Set<EnhancedForStatement> markedFors;
    private final ASTRewrite rewrite;
    private final CompilationUnit cu;
    private final Set<String> usedNames = new java.util.HashSet<>();


    private boolean needsIteratorImport = false;
    private boolean needsArraysImport = false;

    public ForEachToWhileVisitor(Set<EnhancedForStatement> markedFors, ASTRewrite rewrite, CompilationUnit cu) {
        this.markedFors = markedFors;
        this.rewrite = rewrite;
        this.cu = cu;
    }

    @SuppressWarnings("unchecked")
	@Override
    public boolean visit(EnhancedForStatement node) {
        if (!markedFors.contains(node)) return true;

        AST ast = node.getAST();

        // Datos base
        final SingleVariableDeclaration param = node.getParameter();
        final Type paramTypeCopy = (Type) ASTNode.copySubtree(ast, param.getType());
        final Expression iteratedExprCopy1 = (Expression) ASTNode.copySubtree(ast, node.getExpression());
        final ITypeBinding exprBinding = node.getExpression().resolveTypeBinding();

        // ¿Es array?
        boolean isArray = exprBinding != null && exprBinding.isArray();
        boolean isPrimitiveArray = isArray && exprBinding.getElementType().isPrimitive();

        // Preparar estructuras resultantes
        Statement replacementWhile;
        Statement preStatement1 = null; // iterDecl o idxDecl

        if (isArray && isPrimitiveArray) {
            // ======================================================
            // Caso: array primitivo  -> while indexado (sin Iterator)
            // ======================================================
            String idxName = uniqueName("idx", node);

            // int idx = 0;
            VariableDeclarationFragment idxFrag = ast.newVariableDeclarationFragment();
            idxFrag.setName(ast.newSimpleName(idxName));
            NumberLiteral zero = ast.newNumberLiteral("0");
            idxFrag.setInitializer(zero);

            VariableDeclarationStatement idxDecl = ast.newVariableDeclarationStatement(idxFrag);
            idxDecl.setType(ast.newPrimitiveType(PrimitiveType.INT));

            // while (idx < expr.length)
            InfixExpression cond = ast.newInfixExpression();
            cond.setOperator(InfixExpression.Operator.LESS);
            cond.setLeftOperand(ast.newSimpleName(idxName));
            cond.setRightOperand(arrayLengthExpr(ast, (Expression) ASTNode.copySubtree(ast, node.getExpression())));

            WhileStatement whileStmt = ast.newWhileStatement();
            whileStmt.setExpression(cond);

            // cuerpo: T e = expr[idx];  ...body...  idx++;
            Block body = ast.newBlock();

            VariableDeclarationFragment elemFrag = ast.newVariableDeclarationFragment();
            elemFrag.setName((SimpleName) ASTNode.copySubtree(ast, param.getName()));

            ArrayAccess access = ast.newArrayAccess();
            access.setArray((Expression) ASTNode.copySubtree(ast, node.getExpression()));
            access.setIndex(ast.newSimpleName(idxName));
            elemFrag.setInitializer(access);

            VariableDeclarationStatement elemDecl = ast.newVariableDeclarationStatement(elemFrag);
            elemDecl.setType(paramTypeCopy);
            body.statements().add(elemDecl);

            appendBody(ast, body, node.getBody());

            PostfixExpression inc = ast.newPostfixExpression();
            inc.setOperator(PostfixExpression.Operator.INCREMENT);
            inc.setOperand(ast.newSimpleName(idxName));
            body.statements().add(ast.newExpressionStatement(inc));

            whileStmt.setBody(body);

            preStatement1 = idxDecl;
            replacementWhile = whileStmt;

        } else {
            // ======================================================
            // Caso: Iterable o array de referencia -> Iterator<T>
            // ======================================================
            needsIteratorImport = true;

            // Iterator<T> it = (isArrayRef ? Arrays.stream(expr).iterator() : expr.iterator());
            VariableDeclarationFragment iterFrag = ast.newVariableDeclarationFragment();
            String itName = uniqueName("it", node);
            iterFrag.setName(ast.newSimpleName(itName));

            Expression iteratorInit;
            if (isArray) {
                // Arrays.stream(expr).iterator()
                needsArraysImport = true;
                MethodInvocation streamCall = ast.newMethodInvocation();
                streamCall.setExpression(ast.newSimpleName("Arrays"));
                streamCall.setName(ast.newSimpleName("stream"));
                streamCall.arguments().add(iteratedExprCopy1);

                MethodInvocation iterCall = ast.newMethodInvocation();
                iterCall.setExpression(streamCall);
                iterCall.setName(ast.newSimpleName("iterator"));
                iteratorInit = iterCall;
            } else {
                // expr.iterator()
                MethodInvocation iterCall = ast.newMethodInvocation();
                iterCall.setExpression(iteratedExprCopy1);
                iterCall.setName(ast.newSimpleName("iterator"));
                iteratorInit = iterCall;
            }
            iterFrag.setInitializer(iteratorInit);

            VariableDeclarationStatement iterDecl = ast.newVariableDeclarationStatement(iterFrag);

            // Iterator<T>
            Type typeArg = (Type) ASTNode.copySubtree(ast, param.getType());
            if (typeArg.isPrimitiveType()) {
                typeArg = boxedType(ast, (PrimitiveType) typeArg);
            }
            ParameterizedType iterType = ast.newParameterizedType(ast.newSimpleType(ast.newSimpleName("Iterator")));
            //@SuppressWarnings("unchecked")
            java.util.List<Type> typeArgs = iterType.typeArguments();
            typeArgs.add(typeArg);
            iterDecl.setType(iterType);

            // while (it.hasNext()) { T e = it.next(); body }
            MethodInvocation hasNext = ast.newMethodInvocation();
            hasNext.setExpression(ast.newSimpleName(itName));
            hasNext.setName(ast.newSimpleName("hasNext"));

            WhileStatement whileStmt = ast.newWhileStatement();
            whileStmt.setExpression(hasNext);

            Block body = ast.newBlock();

            VariableDeclarationFragment elemFrag = ast.newVariableDeclarationFragment();
            elemFrag.setName((SimpleName) ASTNode.copySubtree(ast, param.getName()));

            MethodInvocation nextCall = ast.newMethodInvocation();
            nextCall.setExpression(ast.newSimpleName(itName));
            nextCall.setName(ast.newSimpleName("next"));
            elemFrag.setInitializer(nextCall);

            VariableDeclarationStatement elemDecl = ast.newVariableDeclarationStatement(elemFrag);
            elemDecl.setType((Type) ASTNode.copySubtree(ast, param.getType()));

            body.statements().add(elemDecl);
            appendBody(ast, body, node.getBody());
            whileStmt.setBody(body);

            preStatement1 = iterDecl;
            replacementWhile = whileStmt;
        }

        // Insertar evitando bloque extra si el padre es un Block
        ASTNode parent = node.getParent();
        if (parent instanceof Block) {
            ListRewrite lr = rewrite.getListRewrite(parent, Block.STATEMENTS_PROPERTY);
            lr.insertBefore(preStatement1, node, null);
            rewrite.replace(node, replacementWhile, null);
        } else {
            Block wrapper = ast.newBlock();
            wrapper.statements().add(preStatement1);
            wrapper.statements().add(replacementWhile);
            rewrite.replace(node, wrapper, null);
        }

        return false;
    }

    @Override
    public void endVisit(CompilationUnit node) {
        AST ast = node.getAST();
        ListRewrite listRewrite = rewrite.getListRewrite(node, CompilationUnit.IMPORTS_PROPERTY);

        if (needsIteratorImport && !hasImport("java.util.Iterator")) {
            ImportDeclaration imp = ast.newImportDeclaration();
            imp.setName(ast.newName("java.util.Iterator"));
            listRewrite.insertLast(imp, null);
        }
        if (needsArraysImport && !hasImport("java.util.Arrays")) {
            ImportDeclaration imp = ast.newImportDeclaration();
            imp.setName(ast.newName("java.util.Arrays"));
            listRewrite.insertLast(imp, null);
        }
    }

    // ========= helpers =========

    @SuppressWarnings("unchecked")
	private void appendBody(AST ast, Block dest, Statement originalBody) {
        if (originalBody instanceof Block b) {
            for (Object s : b.statements()) {
                dest.statements().add(ASTNode.copySubtree(ast, (ASTNode) s));
            }
        } else {
            dest.statements().add(ASTNode.copySubtree(ast, originalBody));
        }
    }

    private boolean hasImport(String fqn) {
        for (Object o : cu.imports()) {
            ImportDeclaration id = (ImportDeclaration) o;
            if (!id.isOnDemand() && id.getName().getFullyQualifiedName().equals(fqn)) return true;
        }
        return false;
    }

    /*
    private String uniqueName(String base) {
        // Sufijo simple para minimizar colisiones. Si necesitás 100% único, podés recorrer nombres del scope.
        return base;
    }
    */

    private String uniqueName(String base, ASTNode context) {
        // Recolecta nombres ya presentes en el bloque original
        usedNames.addAll(collectNames(context));

        String candidate = base;
        int i = 1;
        while (usedNames.contains(candidate)) {
            candidate = base + i;
            i++;
        }
        usedNames.add(candidate); // <-- ahora registramos el nuevo
        return candidate;
    }

   // @SuppressWarnings("unchecked")
    private Set<String> collectNames(ASTNode context) {
        Set<String> names = new java.util.HashSet<>();
        // Busco el bloque contenedor
        ASTNode block = context;
        while (block != null && !(block instanceof Block)) {
            block = block.getParent();
        }
        if (block instanceof Block b) {
            for (Object o : b.statements()) {
                if (o instanceof VariableDeclarationStatement vds) {
                    for (Object f : vds.fragments()) {
                        names.add(((VariableDeclarationFragment) f).getName().getIdentifier());
                    }
                }
            }
        }
        return names;
    }
    
    
    private Expression arrayLengthExpr(AST ast, Expression arrayExpr) {
        if (arrayExpr instanceof Name name) {
            return ast.newQualifiedName((Name) ASTNode.copySubtree(ast, name), ast.newSimpleName("length"));
        } else {
            FieldAccess fa = ast.newFieldAccess();
            fa.setExpression((Expression) ASTNode.copySubtree(ast, arrayExpr));
            fa.setName(ast.newSimpleName("length"));
            return fa;
        }
    }

    private Type boxedType(AST ast, PrimitiveType prim) {
        String wrapper;
        PrimitiveType.Code code = prim.getPrimitiveTypeCode();
        if (code == PrimitiveType.BOOLEAN) wrapper = "Boolean";
        else if (code == PrimitiveType.BYTE) wrapper = "Byte";
        else if (code == PrimitiveType.SHORT) wrapper = "Short";
        else if (code == PrimitiveType.CHAR) wrapper = "Character";
        else if (code == PrimitiveType.INT) wrapper = "Integer";
        else if (code == PrimitiveType.LONG) wrapper = "Long";
        else if (code == PrimitiveType.FLOAT) wrapper = "Float";
        else /* DOUBLE */ wrapper = "Double";
        return ast.newSimpleType(ast.newSimpleName(wrapper));
    }
}
