package org.plugin.refactor.flow;


import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.dom.Expression;

public class ForToWhileVisitor extends ASTVisitor {
    private final Set<ForStatement> markedFors;
    private final ASTRewrite rewriter;

    public ForToWhileVisitor(Set<ForStatement> markedFors, ASTRewrite rewriter) {
        this.markedFors = markedFors;
        this.rewriter = rewriter;
    }

    @SuppressWarnings("unchecked")
	@Override
    public boolean visit(ForStatement node) {
        if (!markedFors.contains(node)) {
            return true; // No esta marcado
        }

        AST ast = node.getAST();

        // Crear el nuevo while
        WhileStatement whileStmt = ast.newWhileStatement();

        // Condicion del while
        Expression condition = node.getExpression() != null
                ? (Expression) ASTNode.copySubtree(ast, node.getExpression())
                : ast.newBooleanLiteral(true);
        whileStmt.setExpression(condition);

        // Copiar body y envolverlo en bloque si es necesario
        Statement originalBody = node.getBody();
        Block newBody;
        if (originalBody instanceof Block) {
            newBody = (Block) ASTNode.copySubtree(ast, originalBody);
        } else {
            newBody = ast.newBlock();
            newBody.statements().add(ASTNode.copySubtree(ast, originalBody));
        }

        // Agregar updaters al final
        for (Object updater : node.updaters()) {
            ExpressionStatement updaterStmt = ast.newExpressionStatement(
                    (Expression) ASTNode.copySubtree(ast, (ASTNode) updater)
            );
            newBody.statements().add(updaterStmt);
        }

        whileStmt.setBody(newBody);

        // Procesar inicializadores
        if (!node.initializers().isEmpty()) {
            insertInitializersBefore(node, ast, node.initializers());
        }

        // Reemplazar el for por el while
        rewriter.replace(node, whileStmt, null);

        return false; // No seguir visitando hijos
    }

    @SuppressWarnings("unchecked")
    private void insertInitializersBefore(ForStatement node, AST ast, List<?> initializers) {
        ASTNode parent = node.getParent();

        if (parent instanceof Block) {
            // Insertar antes en el mismo bloque
            ListRewrite listRewrite = rewriter.getListRewrite(parent, Block.STATEMENTS_PROPERTY);

            for (Object init : initializers) {
                Statement initStmt = createInitStatement(ast, init);
                if (initStmt != null) {
                    listRewrite.insertBefore(initStmt, node, null);
                }
            }
        } else {
            // El for no esta en un bloque: envolverlo en uno
            Block newBlock = ast.newBlock();

            // Inicializadores
            for (Object init : initializers) {
                Statement initStmt = createInitStatement(ast, init);
                if (initStmt != null) {
                    newBlock.statements().add(initStmt);
                }
            }

            // Insertar el while despues
            newBlock.statements().add(ASTNode.copySubtree(ast, node));
            rewriter.replace(node, newBlock, null);
        }
    }

    @SuppressWarnings("unchecked")
    private Statement createInitStatement(AST ast, Object init) {
        if (init instanceof VariableDeclarationExpression) {
            VariableDeclarationExpression varExpr = (VariableDeclarationExpression) init;

            // Crear un VariableDeclarationStatement vacio
            VariableDeclarationStatement varDeclStmt = ast.newVariableDeclarationStatement(ast.newVariableDeclarationFragment());

            // Limpiar fragmento dummy
            varDeclStmt.fragments().clear();

            // Copiar todos los fragmentos originales
            for (Object fragObj : varExpr.fragments()) {
                VariableDeclarationFragment fragCopy = (VariableDeclarationFragment)
                        ASTNode.copySubtree(ast, (ASTNode) fragObj);
                varDeclStmt.fragments().add(fragCopy);
            }

            // Copiar el tipo
            varDeclStmt.setType((Type) ASTNode.copySubtree(ast, varExpr.getType()));

            return varDeclStmt;
        } 
        else if (init instanceof Expression) {
            return ast.newExpressionStatement((Expression) ASTNode.copySubtree(ast, (Expression) init));
        }
        return null;
    }
}
