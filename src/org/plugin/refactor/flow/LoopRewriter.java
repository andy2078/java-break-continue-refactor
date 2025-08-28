package org.plugin.refactor.flow;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;


public class LoopRewriter extends ASTVisitor {
	
    private final ASTRewrite rewrite;

    public LoopRewriter(ASTRewrite rewrite) {
        this.rewrite = rewrite;
    }

    @Override
    public void endVisit(BreakStatement node) {
        String nombre = (String) node.getProperty("breakName");
        if (nombre != null) {
            AST ast = node.getAST();
            Assignment asign = ast.newAssignment();
            asign.setLeftHandSide(ast.newSimpleName(nombre));
            asign.setRightHandSide(ast.newBooleanLiteral(false));
            asign.setOperator(Assignment.Operator.ASSIGN);

            ExpressionStatement reemplazo = ast.newExpressionStatement(asign);
            rewrite.replace(node, reemplazo, null);
        }
    }

    @Override
  public void endVisit(ContinueStatement node) {
      String nombre = (String) node.getProperty("continueName");
      if (nombre != null) {
          AST ast = node.getAST();
          Assignment asign = ast.newAssignment();
          asign.setLeftHandSide(ast.newSimpleName(nombre));
          asign.setRightHandSide(ast.newBooleanLiteral(false));
          asign.setOperator(Assignment.Operator.ASSIGN);

          ExpressionStatement reemplazo = ast.newExpressionStatement(asign);
          rewrite.replace(node, reemplazo, null);
      }
  }
    
    
    @Override
    public boolean visit(WhileStatement node) {
        processLoop(node);
        return true;
    }

    @Override
    public boolean visit(DoStatement node) {
        processLoop(node);
        return true;
    }

    private void processLoop(Statement loopNode) {
        String name = (String) loopNode.getProperty("breakName");
        String continueName = (String) loopNode.getProperty("continueName");
        if ((name == null) && (continueName == null)) 
        	return;

        AST ast = loopNode.getAST();

        if (name != null) {
            // crear: boolean nombre = true;
            VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
            frag.setName(ast.newSimpleName(name));
            frag.setInitializer(ast.newBooleanLiteral(true));
            VariableDeclarationStatement decl = ast.newVariableDeclarationStatement(frag);
            decl.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));

            // insertar declaracion antes del loop o envolver en Block si el parent no es Block
            insertDeclarationBefore(loopNode, decl);

            // modificar la condicion: nombre && (condOriginal)
            Expression originalCond = null;
            if (loopNode instanceof WhileStatement) {
                originalCond = ((WhileStatement) loopNode).getExpression();
            } else if (loopNode instanceof DoStatement) {
                originalCond = ((DoStatement) loopNode).getExpression();
            }

            if (originalCond != null) {
                InfixExpression newCond = ast.newInfixExpression();
                newCond.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
                newCond.setLeftOperand(ast.newSimpleName(name));

                ParenthesizedExpression paren = ast.newParenthesizedExpression();
                // usar createCopyTarget para que ASTRewrite maneje la copia
                paren.setExpression((Expression) rewrite.createCopyTarget(originalCond));
                newCond.setRightOperand(paren);

                rewrite.replace(originalCond, newCond, null);
            }
        }
        
        if (continueName != null) {
            // crear: boolean nombre = true;
            VariableDeclarationFragment frag = ast.newVariableDeclarationFragment();
            frag.setName(ast.newSimpleName(continueName));
            frag.setInitializer(ast.newBooleanLiteral(true));
            VariableDeclarationStatement decl = ast.newVariableDeclarationStatement(frag);
            decl.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));

            // insertar declaracion despues del loop 
            insertDeclarationFirst(loopNode, decl);
        }
    }

    private void insertDeclarationFirst(Statement loopNode, VariableDeclarationStatement decl) {
        ASTNode body = getBody(loopNode);

        if (body != null) {
            // Caso 1: el cuerpo ya es un bloque { ... }
            ListRewrite lr = rewrite.getListRewrite(body, Block.STATEMENTS_PROPERTY);
            lr.insertFirst(decl, null);
        } 
    }

    
    @SuppressWarnings("unchecked")
	private void insertDeclarationBefore(Statement loopNode, VariableDeclarationStatement decl) {
        AST ast = loopNode.getAST();
        ASTNode parent = loopNode.getParent();

        if (parent instanceof Block) {
            ListRewrite listRewrite = rewrite.getListRewrite(parent, Block.STATEMENTS_PROPERTY);
            listRewrite.insertBefore(decl, loopNode, null);
        } else {
            Block newBlock = ast.newBlock();
            newBlock.statements().add(ASTNode.copySubtree(ast, decl));
            newBlock.statements().add(ASTNode.copySubtree(ast, loopNode));
            rewrite.replace(loopNode, newBlock, null);
        }
    }

    /**
     * Agrupar sentencias marcadas con "move" en if (name) { ... }
     * Ejecutado en endVisit para que ya se hayan reemplazado los break.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean visit(Block node) {
    	
    	List<Statement> stmts = (List<Statement>) node.statements();
        int n = stmts.size();
        int i = 0;
        while (i < n) {
            Statement s = stmts.get(i);
            String moveAfterBreak = (String) s.getProperty("moveAfterBreak");
            String moveAfterContinue = (String) s.getProperty("moveAfterContinue");
            if (moveAfterBreak == null && moveAfterContinue == null) {
                i++;
                continue;
            }
            int j = i;
            
            if (moveAfterContinue == null) {
	            
	            List<Statement> group = new ArrayList<>();
	            while (j < n) {
	                Statement s2 = stmts.get(j);
	                    group.add(s2);
	                    j++;
	            }
	            groupIntoIf(node, moveAfterBreak);
            }
            
            if (moveAfterBreak == null) {
	            
	            List<Statement> group = new ArrayList<>();
	            while (j < n) {
	                Statement s2 = stmts.get(j);
	                    group.add(s2);
	                    j++;
	            }
	            groupIntoIf(node, moveAfterContinue);
            }
            
            if (moveAfterBreak != null && moveAfterContinue != null) {
	            // comenzar grupo
	            List<Statement> group = new ArrayList<>();
	            while (j < n) {
	                Statement s2 = stmts.get(j);
	                    group.add(s2);
	                    j++;
	            }
	            groupIntoIf(node, moveAfterBreak, moveAfterContinue); 
            }
            i = j;
        }
        return true;
    }

    public Block getBody(Statement node) {
    	Block block = null;
    	if (node instanceof WhileStatement ws && ws.getBody() instanceof Block)
    		block = (Block) ws.getBody();
    	if (node instanceof DoStatement ds && ds.getBody() instanceof Block)
    		block = (Block) ds.getBody();

    	return block;
    }
    

    @SuppressWarnings("unchecked")
    private void groupIntoIf(Block block, String varName) {
        AST ast = block.getAST();
        ListRewrite lr = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);

        // Copia para no iterar y modificar la misma lista a la vez
        List<Statement> original = new ArrayList<>((List<Statement>) block.statements());

        for (Statement s : original) {
            if (varName.equals(s.getProperty("moveAfterBreak"))) {
                IfStatement ifStmt = ast.newIfStatement();
                ifStmt.setExpression(ast.newSimpleName(varName));

                Block thenBlock = ast.newBlock();

                // ⬇️ clave: copiar con placeholder del rewrite, no con copySubtree
                Statement copyWithChanges = (Statement) rewrite.createCopyTarget(s);
                thenBlock.statements().add(copyWithChanges);

                ifStmt.setThenStatement(thenBlock);

                // Reemplaza la sentencia original por el if(...){ copiaConCambios }
                lr.replace(s, ifStmt, null);
            }
            else if (varName.equals(s.getProperty("moveAfterContinue"))) {
                IfStatement ifStmt = ast.newIfStatement();
                ifStmt.setExpression(ast.newSimpleName(varName));

                Block thenBlock = ast.newBlock();

                // ⬇️ clave: copiar con placeholder del rewrite, no con copySubtree
                Statement copyWithChanges = (Statement) rewrite.createCopyTarget(s);
                thenBlock.statements().add(copyWithChanges);

                ifStmt.setThenStatement(thenBlock);

                // Reemplaza la sentencia original por el if(...){ copiaConCambios }
                lr.replace(s, ifStmt, null);
            }
        }
    }
    
    
    @SuppressWarnings("unchecked")
    private void groupIntoIf(Block bloque, String varName1, String varName2) {
        AST ast = bloque.getAST();
        ListRewrite lr = rewrite.getListRewrite(bloque, Block.STATEMENTS_PROPERTY);

        // copia para no iterar y modificar la misma lista a la vez
        List<Statement> original = new ArrayList<>((List<Statement>) bloque.statements());

        for (Statement s : original) {
            Object prop = s.getProperty("moveAfterBreak");
            if (prop == null) 
            	continue;

            String move = prop.toString();
            // agrupamos si coincide con alguna de las dos variables
            if (move.equals(varName1) || move.equals(varName2)) {
                // Construir if (varName1 && varName2)
                IfStatement ifStmt = ast.newIfStatement();

                InfixExpression cond = ast.newInfixExpression();
                cond.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
                cond.setLeftOperand(ast.newSimpleName(varName1));
                cond.setRightOperand(ast.newSimpleName(varName2));

                ifStmt.setExpression(cond);

                Block thenBlock = ast.newBlock();

                // Copiamos la sentencia marcada
                Statement copyWithChanges = (Statement) rewrite.createCopyTarget(s);
                thenBlock.statements().add(copyWithChanges);

                ifStmt.setThenStatement(thenBlock);

                // Reemplaza la sentencia original por el if(...){ copia }
                lr.replace(s, ifStmt, null);
            }
        }
    }
}
