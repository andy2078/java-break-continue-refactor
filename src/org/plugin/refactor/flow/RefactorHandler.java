package org.plugin.refactor.flow;


import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
//import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.text.Document;
//import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
//import org.plugin.refactor.ej27.RefactorVisitor;
//import org.plugin.refactor.ej18.MyASTVisitor;
//import org.eclipse.jface.viewers.ISelection;
//import org.eclipse.jface.viewers.IStructuredSelection;
//import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class RefactorHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        try {
            IEditorPart editor = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow()
                    .getActivePage()
                    .getActiveEditor();

            if (!(editor instanceof ITextEditor)) {
                return null;
            }

            ICompilationUnit unit = (ICompilationUnit) JavaUI.getEditorInputJavaElement(editor.getEditorInput());

            // ====================
            // Parseamos el AST
            // ====================
            ASTParser parser = ASTParser.newParser(AST.JLS21);
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setSource(unit);
            parser.setResolveBindings(true);

            CompilationUnit cu = (CompilationUnit) parser.createAST(null);

            // ====================
            // Etapa 0a: For → While
            // ====================
            ForMarkerVisitor forMarker = new ForMarkerVisitor(cu);
            cu.accept(forMarker);

            if (!forMarker.getMarkedFors().isEmpty()) {
                ASTRewrite forRewrite = ASTRewrite.create(cu.getAST());
                ForToWhileVisitor forConverter = new ForToWhileVisitor(forMarker.getMarkedFors(), forRewrite);
                cu.accept(forConverter);

                Document document = new Document(unit.getSource());
                TextEdit edits = forRewrite.rewriteAST(document, null);
                edits.apply(document);

                unit.getBuffer().setContents(document.get());
                unit.save(null, true);

                // volver a parsear el AST para que se actualice
                parser.setSource(unit);
                cu = (CompilationUnit) parser.createAST(null);
            }

            // ====================
            // Etapa 0b: ForEach → While con Iterator
            // ====================
            ForEachMarkerVisitor foreachMarker = new ForEachMarkerVisitor(cu);
            cu.accept(foreachMarker);

            if (!foreachMarker.getMarkedFors().isEmpty()) {
                ASTRewrite foreachRewrite = ASTRewrite.create(cu.getAST());
                ForEachToWhileVisitor foreachConverter =
                        new ForEachToWhileVisitor(foreachMarker.getMarkedFors(), foreachRewrite, cu);
                cu.accept(foreachConverter);

                Document document = new Document(unit.getSource());
                TextEdit edits = foreachRewrite.rewriteAST(document, null);
                edits.apply(document);

                unit.getBuffer().setContents(document.get());
                unit.save(null, true);

                // volver a parsear el AST para que la siguiente etapa lo tome con los cambios
                parser.setSource(unit);
                cu = (CompilationUnit) parser.createAST(null);
            }

            // ====================
            // Etapa 1: Anotar breaks/continues
            // ====================
            FlowAnnotator annotator = new FlowAnnotator(cu);
            cu.accept(annotator);

            // ====================
            // Etapa 2: Refactorizar loops anotados
            // ====================
            ASTRewrite rewrite = ASTRewrite.create(cu.getAST());
            LoopRewriter rewriter = new LoopRewriter(rewrite, unit);
            cu.accept(rewriter);

            // =================================
            // Postprocesamiento para los ifs multiples
            // ================================

            Document document = new Document(unit.getSource());
            TextEdit edits = rewrite.rewriteAST(document, null);
            edits.apply(document);

            // Actualizar unidad de compilación
            unit.getBuffer().setContents(document.get());
            unit.save(null, true);

            // Paso 2: volver a parsear para tener un AST actualizado
            parser.setSource(unit);
            cu = (CompilationUnit) parser.createAST(null);

            // Paso 3: ejecutar Visitors en secuencia
            ASTRewrite rewriter2 = ASTRewrite.create(cu.getAST());
            cu.accept(new IfAnnotatorVisitor());
            cu.accept(new IfMergerVisitor(rewriter2));

            Document document2 = new Document(unit.getSource());
            TextEdit edits2 = rewriter2.rewriteAST(document2, null);
            edits2.apply(document2);

            unit.getBuffer().setContents(document2.get());
            unit.save(null, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
