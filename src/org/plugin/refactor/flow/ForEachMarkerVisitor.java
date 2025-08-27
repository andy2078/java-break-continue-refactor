package org.plugin.refactor.flow;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.ASTVisitor;

public class ForEachMarkerVisitor extends ASTVisitor {

    private final Set<EnhancedForStatement> markedFors = new HashSet<>();
//    private CompilationUnit cu;
    
    public ForEachMarkerVisitor(CompilationUnit cu) {
//    	this.cu = cu;
    }
    

    @Override
    public boolean visit(BreakStatement node) {
        markForEachParent(node);
        return super.visit(node);
    }

    @Override
    public boolean visit(ContinueStatement node) {
        markForEachParent(node);
        return super.visit(node);
    }

    /**
     * Busca hacia arriba en el arbol hasta encontrar el EnhancedForStatement padre
     * y lo marca para la conversion.
     */
    private void markForEachParent(Statement stmt) {
        ASTNode parent = stmt.getParent();
        while (parent != null && !(parent instanceof ForStatement)
        		&& !(parent instanceof WhileStatement) && !(parent instanceof DoStatement)
        		&& !(parent instanceof EnhancedForStatement)) {
            		parent = parent.getParent();
        }
        if (parent instanceof EnhancedForStatement) {
            markedFors.add((EnhancedForStatement) parent);
        }
    }

    public Set<EnhancedForStatement> getMarkedFors() {
        return markedFors;
    }
}
