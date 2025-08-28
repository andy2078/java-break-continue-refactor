package org.plugin.refactor.flow;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

public class IfMergerVisitor extends ASTVisitor {
    private final ASTRewrite rewriter;

    public IfMergerVisitor(ASTRewrite rewriter) {
        this.rewriter = rewriter;
    }

    @SuppressWarnings("unchecked")
	@Override
    public boolean visit(Block node) {
        List<?> statements = node.statements();
        AST ast = node.getAST();

        for (int i = 0; i < statements.size(); i++) {
            Statement stmt = (Statement) statements.get(i);
            if (!(stmt instanceof IfStatement)) 
            	continue;

            IfStatement ifStmt = (IfStatement) stmt;
            Object group = ifStmt.getProperty("mergeGroup");

            if (group != null) {
                // recolectar todos los If con mismo mergeGroup seguidos
                List<Statement> mergedStatements = new ArrayList<>();
                Expression condition = ifStmt.getExpression();
                int j = i;
                while (j < statements.size()) {
                    Statement next = (Statement) statements.get(j);
                    if (next instanceof IfStatement
                            && group.equals(((IfStatement) next).getProperty("mergeGroup"))) {
                        Statement thenStmt = ((IfStatement) next).getThenStatement();
                        if (thenStmt instanceof Block) {
                            mergedStatements.addAll(((Block) thenStmt).statements());
                        } else {
                            mergedStatements.add(thenStmt);
                        }
                        j++;
                    } else break;
                }

                // crear nuevo If con bloque unificado
                Block newBlock = ast.newBlock();
                newBlock.statements().addAll(ASTNode.copySubtrees(ast, mergedStatements));
                IfStatement newIf = ast.newIfStatement();
                newIf.setExpression((Expression) ASTNode.copySubtree(ast, condition));
                newIf.setThenStatement(newBlock);

                // reemplazar el rango [i, j)
                ListRewrite listRewrite = rewriter.getListRewrite(node, Block.STATEMENTS_PROPERTY);
                for (int k = i; k < j; k++) {
                    listRewrite.remove((Statement) statements.get(k), null);
                }
                listRewrite.insertAt(newIf, i, null);

                // saltar al siguiente despues del merge
                i = j - 1;
            }
        }
        return super.visit(node);
    }
}
