package org.plugin.refactor.flow;

import java.util.List;
import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Statement;

public class IfAnnotatorVisitor extends ASTVisitor {
    private int groupCounter = 0;

    @Override
    public boolean visit(Block node) {

        List<?> statements = node.statements();
        int currentGroup = 0;
        Expression lastCondition = null;
        for (int i = 0; i < statements.size(); i++) {
        	
            Statement stmt = (Statement) statements.get(i);
            if (stmt instanceof IfStatement) {
                IfStatement ifStmt = (IfStatement) stmt;
                Expression cond = ifStmt.getExpression();

                if (lastCondition != null && cond.subtreeMatch(new ASTMatcher(), lastCondition)) {
                    // misma condición → mismo grupo
                    if (currentGroup == 0) {
                        currentGroup = ++groupCounter; // arrancamos nuevo grupo
                        // marcar tambien al anterior
                        ((IfStatement) statements.get(i - 1)).setProperty("mergeGroup", currentGroup);
                    }
                    ifStmt.setProperty("mergeGroup", currentGroup);
                } else {
                    // condicion distinta, reinicio
                    currentGroup = 0;
                }

                lastCondition = cond;
            } else {
                currentGroup = 0;
                lastCondition = null;
            }
        }
        return super.visit(node);
    }
}
