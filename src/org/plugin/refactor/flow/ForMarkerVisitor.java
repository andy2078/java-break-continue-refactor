package org.plugin.refactor.flow;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class ForMarkerVisitor extends ASTVisitor {
    private final Set<ForStatement> markedFors = new HashSet<>();
    
    public ForMarkerVisitor() { 
    }

    @Override
    public boolean visit(BreakStatement node) {
        markForParent(node);
        return super.visit(node);
    }

    @Override
    public boolean visit(ContinueStatement node) {
        markForParent(node);
        return super.visit(node);
    }

    private void markForParent(Statement stmt) {
        ASTNode parent = stmt.getParent();
        while (parent != null && !(parent instanceof ForStatement)
        		&& !(parent instanceof WhileStatement) && !(parent instanceof DoStatement)) {
            parent = parent.getParent();
        }
        if (parent instanceof ForStatement) {
            markedFors.add((ForStatement) parent);
        }
    }

    public Set<ForStatement> getMarkedFors() {
        return markedFors;
    }
}
