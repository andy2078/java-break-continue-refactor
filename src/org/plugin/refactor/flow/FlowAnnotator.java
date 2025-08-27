package org.plugin.refactor.flow;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WhileStatement;

public class FlowAnnotator extends ASTVisitor {

    private final Set<String> usedNames = new HashSet<>();
    private final Set<String> existingNames = new HashSet<>(); // variables/parametros ya declarados

    public FlowAnnotator(CompilationUnit cu) {
        // Recolectar nombres existentes para evitar colisiones 
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(VariableDeclarationFragment node) {
                existingNames.add(node.getName().getIdentifier());
                return true;
            }

            @Override
            public boolean visit(SingleVariableDeclaration node) {
                existingNames.add(node.getName().getIdentifier());
                return true;
            }
        });
    }

    @Override
     public boolean visit(BreakStatement node) {
        // buscar el while o do-while padre mas cercano
        ASTNode actual = node.getParent();
        while (actual != null 
                && !(actual instanceof WhileStatement) 
                && !(actual instanceof DoStatement) 
                && !(actual instanceof SwitchStatement)) {
            actual = actual.getParent();
        }

        if (actual == null || actual instanceof SwitchStatement) {
            return false; // es un break de switch, no se procesa
        }

        ASTNode loopParent = actual;

        // obtener o asignar nombre
        String name = (String) loopParent.getProperty("breakName");
        if (name == null) {
            name = createName("stay");
            loopParent.setProperty("breakName", name);
        }

        // marcar el break y las sentencias posteriores
        node.setProperty("breakName", name);
        markFollowingStatements(node, "moveAfterBreak", name);

        return true; // se sigue recorriendo
    }

    
    @Override
    public boolean visit(ContinueStatement node) {
        ASTNode actual = node.getParent();
        while (actual != null && !(actual instanceof WhileStatement) && !(actual instanceof DoStatement)) {
            actual = actual.getParent();
        }
        if (actual == null) {
            return true; 
        }
        ASTNode loopParent = actual;

        // obtener o asignar nombre
        String name = (String) loopParent.getProperty("continueName");
        if (name == null) {
            name = createName("keep");
            loopParent.setProperty("continueName", name);
        }

        // marcar el continue y las sentencias posteriores
        node.setProperty("continueName", name);
        markFollowingStatements(node, "moveAfterContinue", name);

        return true; // se continua visitando
    }
    
    private String createName(String base) {
        int i = 1;
        String candidate;
        do {
            candidate = base + i++;
        } while (usedNames.contains(candidate) || existingNames.contains(candidate));
        usedNames.add(candidate);
        return candidate;
    }

    
    /*
     * En uso actual
     */
    @SuppressWarnings("unchecked")
    private void markFollowingStatements(Statement breakStmt, String property, String name) {
        ASTNode parent = breakStmt.getParent();
        ASTNode son = breakStmt;
        boolean end = false;
        
        while (parent != null && !end) {
        	
        	end =     (parent instanceof WhileStatement 
                    || parent instanceof DoStatement 
                    || parent instanceof ForStatement 
                    || parent instanceof EnhancedForStatement);

        	if (!end) {
            	if (parent instanceof Block block) {
                    List<Statement> stmts = (List<Statement>) block.statements();
                    int index = stmts.indexOf(son);
                    if (index >= 0 && index < (stmts.size() - 1)) {
                        // Marcar todas las sentencias que estan despues del break
                        for (int i = index + 1; i < stmts.size(); i++) {
                            stmts.get(i).setProperty(property, name);
                        }
                    }
            	}
                if (parent instanceof CatchClause catchClause) {
                    Block body = catchClause.getBody();
                    List<Statement> statements = (List<Statement>) body.statements();
                    int idx = statements.indexOf(son);

                    if (idx >= 0 && idx < (statements.size() - 1)) {
                        for (int i = idx + 1; i < statements.size(); i++) {
                            statements.get(i).setProperty(property, name);
                        }
                    } 
                }
                son = parent;
                parent = son.getParent();
        	}
        }
        
    }
}
