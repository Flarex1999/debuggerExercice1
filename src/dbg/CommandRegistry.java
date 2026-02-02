package dbg;

import java.util.HashMap;
import java.util.Map;

// Registre des commandes - gere les commandes simples et avec parametres
public class CommandRegistry {

    private Map<String, Command> commands;
    private Map<String, CommandFactory> factories;

    public CommandRegistry() {
        this.commands = new HashMap<>();
        this.factories = new HashMap<>();
    }

    // Enregistre une commande simple (sans parametres)
    public void register(String commandName, Command command) {
        commands.put(commandName, command);
    }

    // Enregistre une fabrique de commandes (pour les commandes avec parametres)
    public void registerFactory(String commandName, CommandFactory factory) {
        factories.put(commandName, factory);
    }

    // Recupere une commande simple
    public Command get(String commandName) {
        return commands.get(commandName);
    }

    // Verifie si la commande existe (simple ou fabrique)
    public boolean hasCommand(String input) {
        String cmdName = input.split("\\s+")[0];
        return commands.containsKey(cmdName) || factories.containsKey(cmdName);
    }

    // Cree et retourne la commande appropriee selon l'input
    public Command getCommand(String input) {
        String[] parts = input.split("\\s+");
        String cmdName = parts[0];

        // Commande simple
        if (commands.containsKey(cmdName)) {
            return commands.get(cmdName);
        }

        // Commande avec parametres
        if (factories.containsKey(cmdName)) {
            String[] args = new String[parts.length - 1];
            System.arraycopy(parts, 1, args, 0, args.length);
            return factories.get(cmdName).create(args);
        }

        return null;
    }

    // Interface pour les fabriques de commandes
    public interface CommandFactory {
        Command create(String[] args);
    }
}
