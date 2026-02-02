package gui;

import com.sun.jdi.*;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

// Panneau INSPECTOR qui affiche les variables sous forme d'arbre
// Un objet = noeud racine avec son type
// Ses variables d'instance = feuilles (qui peuvent avoir des sous-feuilles)
public class InspectorPanel extends JPanel {

    private JTree variableTree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;

    // Limite de profondeur pour eviter les boucles infinies
    private static final int MAX_DEPTH = 3;

    public InspectorPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("INSPECTOR"));

        // Cree l'arbre avec un noeud racine
        rootNode = new DefaultMutableTreeNode("Variables");
        treeModel = new DefaultTreeModel(rootNode);
        variableTree = new JTree(treeModel);
        variableTree.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Style de l'arbre
        variableTree.setRootVisible(true);
        variableTree.setShowsRootHandles(true);

        add(new JScrollPane(variableTree), BorderLayout.CENTER);
        setPreferredSize(new Dimension(300, 200));
    }

    // Met a jour l'arbre avec les variables de la frame selectionnee
    public void updateVariables(StackFrame frame) {
        rootNode.removeAllChildren();

        if (frame == null) {
            treeModel.reload();
            return;
        }

        try {
            // 1. Ajoute "this" si c'est une methode d'instance
            ObjectReference thisObj = frame.thisObject();
            if (thisObj != null) {
                // Noeud racine = "this : TypeDeLObjet"
                DefaultMutableTreeNode thisNode = createObjectNode("this", thisObj, 0);
                rootNode.add(thisNode);
            }

            // 2. Ajoute les variables locales (temporaries)
            List<LocalVariable> locals = frame.visibleVariables();
            for (LocalVariable var : locals) {
                Value value = frame.getValue(var);
                DefaultMutableTreeNode varNode = createValueNode(var.name(), var.typeName(), value, 0);
                rootNode.add(varNode);
            }

        } catch (AbsentInformationException e) {
            rootNode.add(new DefaultMutableTreeNode("(no debug info - compile with -g)"));
        } catch (Exception e) {
            rootNode.add(new DefaultMutableTreeNode("Error: " + e.getMessage()));
        }

        treeModel.reload();
        expandAllNodes();
    }

    // Cree un noeud pour un OBJET (avec ses variables d'instance comme enfants)
    // Format: "nomVariable : TypeDeLObjet"
    private DefaultMutableTreeNode createObjectNode(String name, ObjectReference obj, int depth) {
        // Noeud racine affiche le TYPE de l'objet
        String typeName = obj.referenceType().name();
        // Simplifie le nom du type (enleve le package si trop long)
        String simpleTypeName = simplifyTypeName(typeName);

        String label = name + " : " + simpleTypeName;
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);

        // Limite la profondeur pour eviter les boucles infinies
        if (depth >= MAX_DEPTH) {
            node.add(new DefaultMutableTreeNode("(max depth reached)"));
            return node;
        }

        try {
            ReferenceType type = obj.referenceType();

            // Recupere TOUTES les variables d'instance (champs)
            List<Field> fields = type.allFields();
            Map<Field, Value> values = obj.getValues(fields);

            for (Field field : fields) {
                // Ignore les champs statiques (on veut que les variables d'instance)
                if (field.isStatic()) continue;

                Value value = values.get(field);
                String fieldTypeName = field.typeName();

                // Cree un noeud enfant pour chaque variable d'instance
                DefaultMutableTreeNode childNode = createValueNode(field.name(), fieldTypeName, value, depth + 1);
                node.add(childNode);
            }

            // Si pas de champs, indique que l'objet est vide
            if (node.getChildCount() == 0) {
                node.add(new DefaultMutableTreeNode("(no instance variables)"));
            }

        } catch (Exception e) {
            node.add(new DefaultMutableTreeNode("(error: " + e.getMessage() + ")"));
        }

        return node;
    }

    // Cree un noeud pour une VALEUR (peut etre primitive ou objet)
    private DefaultMutableTreeNode createValueNode(String name, String typeName, Value value, int depth) {

        // Cas 1: valeur null
        if (value == null) {
            return new DefaultMutableTreeNode(name + " : " + simplifyTypeName(typeName) + " = null");
        }

        // Cas 2: c'est une String (cas special - on affiche la valeur)
        if (value instanceof StringReference) {
            String strValue = ((StringReference) value).value();
            // Limite la longueur
            if (strValue.length() > 50) {
                strValue = strValue.substring(0, 50) + "...";
            }
            return new DefaultMutableTreeNode(name + " : String = \"" + strValue + "\"");
        }

        // Cas 3: c'est un objet (pas une primitive) -> noeud avec enfants
        if (value instanceof ObjectReference) {
            return createObjectNode(name, (ObjectReference) value, depth);
        }

        // Cas 4: c'est une primitive (int, boolean, etc.) -> feuille simple
        String simpleType = simplifyTypeName(typeName);
        return new DefaultMutableTreeNode(name + " : " + simpleType + " = " + value.toString());
    }

    // Simplifie un nom de type (enleve le package)
    // Ex: "java.lang.String" -> "String"
    private String simplifyTypeName(String fullTypeName) {
        int lastDot = fullTypeName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullTypeName.substring(lastDot + 1);
        }
        return fullTypeName;
    }

    // Deplie tous les noeuds de l'arbre
    private void expandAllNodes() {
        for (int i = 0; i < variableTree.getRowCount(); i++) {
            variableTree.expandRow(i);
        }
    }

    // Efface l'arbre
    public void clear() {
        rootNode.removeAllChildren();
        treeModel.reload();
    }
}
