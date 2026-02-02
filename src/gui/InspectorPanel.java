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

    // Limite de profondeur pour eviter les arbres gigantesques
    private static final int MAX_DEPTH = 2;

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
        // Deplie seulement le premier niveau (pas tout l'arbre)
        variableTree.expandRow(0);
    }

    // Cree un noeud pour un OBJET (avec ses variables d'instance comme enfants)
    private DefaultMutableTreeNode createObjectNode(String name, ObjectReference obj, int depth) {
        String typeName = obj.referenceType().name();
        String simpleTypeName = simplifyTypeName(typeName);

        // Pour les classes JDK (java.*, javax.*, sun.*), on affiche juste la valeur
        // sans explorer leurs champs internes
        if (isJdkClass(typeName)) {
            String display = getSimpleDisplay(obj, simpleTypeName);
            return new DefaultMutableTreeNode(name + " : " + simpleTypeName + " = " + display);
        }

        // Noeud racine affiche le TYPE de l'objet
        String label = name + " : " + simpleTypeName;
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(label);

        // Limite la profondeur
        if (depth >= MAX_DEPTH) {
            node.add(new DefaultMutableTreeNode("..."));
            return node;
        }

        try {
            ReferenceType type = obj.referenceType();

            // Utilise fields() au lieu de allFields() pour avoir seulement
            // les champs declares dans cette classe (pas les herites)
            List<Field> fields = type.fields();
            Map<Field, Value> values = obj.getValues(fields);

            for (Field field : fields) {
                // Ignore les champs statiques
                if (field.isStatic()) continue;

                Value value = values.get(field);
                String fieldTypeName = field.typeName();

                DefaultMutableTreeNode childNode = createValueNode(field.name(), fieldTypeName, value, depth + 1);
                node.add(childNode);
            }

            if (node.getChildCount() == 0) {
                node.add(new DefaultMutableTreeNode("(empty)"));
            }

        } catch (Exception e) {
            node.add(new DefaultMutableTreeNode("(error)"));
        }

        return node;
    }

    // Cree un noeud pour une VALEUR (primitive ou objet)
    private DefaultMutableTreeNode createValueNode(String name, String typeName, Value value, int depth) {

        // Cas 1: valeur null
        if (value == null) {
            return new DefaultMutableTreeNode(name + " : " + simplifyTypeName(typeName) + " = null");
        }

        // Cas 2: String - affiche directement la valeur
        if (value instanceof StringReference) {
            String strValue = ((StringReference) value).value();
            if (strValue.length() > 40) {
                strValue = strValue.substring(0, 40) + "...";
            }
            return new DefaultMutableTreeNode(name + " : String = \"" + strValue + "\"");
        }

        // Cas 3: Tableau - affiche le type et la taille
        if (value instanceof ArrayReference) {
            ArrayReference arr = (ArrayReference) value;
            String simpleType = simplifyTypeName(typeName);
            return new DefaultMutableTreeNode(name + " : " + simpleType + " (length=" + arr.length() + ")");
        }

        // Cas 4: Objet - cree un noeud avec enfants (sauf pour JDK)
        if (value instanceof ObjectReference) {
            return createObjectNode(name, (ObjectReference) value, depth);
        }

        // Cas 5: Primitive (int, boolean, etc.)
        String simpleType = simplifyTypeName(typeName);
        return new DefaultMutableTreeNode(name + " : " + simpleType + " = " + value.toString());
    }

    // Verifie si c'est une classe du JDK (qu'on ne veut pas explorer en profondeur)
    private boolean isJdkClass(String fullTypeName) {
        return fullTypeName.startsWith("java.") ||
               fullTypeName.startsWith("javax.") ||
               fullTypeName.startsWith("sun.") ||
               fullTypeName.startsWith("jdk.") ||
               fullTypeName.startsWith("com.sun.");
    }

    // Retourne un affichage simple pour les objets JDK
    private String getSimpleDisplay(ObjectReference obj, String simpleType) {
        try {
            // Pour certains types, on peut afficher une valeur utile
            String fullType = obj.referenceType().name();

            if (fullType.equals("java.lang.Integer") ||
                fullType.equals("java.lang.Long") ||
                fullType.equals("java.lang.Double") ||
                fullType.equals("java.lang.Float") ||
                fullType.equals("java.lang.Boolean") ||
                fullType.equals("java.lang.Character")) {
                // Wrapper types - invoke toString()
                return obj.toString();
            }

            // Pour les autres, affiche juste l'id
            return "(id=" + obj.uniqueID() + ")";

        } catch (Exception e) {
            return "(?)";
        }
    }

    // Simplifie un nom de type (enleve le package)
    private String simplifyTypeName(String fullTypeName) {
        int lastDot = fullTypeName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullTypeName.substring(lastDot + 1);
        }
        return fullTypeName;
    }

    // Efface l'arbre
    public void clear() {
        rootNode.removeAllChildren();
        treeModel.reload();
    }
}
