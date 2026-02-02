package dbg;

// Classe de test avec des objets pour tester l'Inspector
public class TestDebuggee {

    public static void main(String[] args) {
        System.out.println("Starting test...");

        // Variables primitives
        int count = 10;
        double price = 19.99;
        boolean active = true;

        // String
        String name = "Test Product";

        // Objet personnalise
        Person person = new Person("Alice", 25);

        // Appel de methode
        person.greet();

        System.out.println("Count: " + count);
        System.out.println("Done!");
    }
}

// Classe simple pour tester l'arbre Inspector
class Person {
    private String name;
    private int age;
    private Address address;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
        this.address = new Address("Paris", "75001");
    }

    public void greet() {
        System.out.println("Hello, I am " + name + ", " + age + " years old");
        System.out.println("I live in " + address.city);
    }
}

// Classe imbriquee pour tester la profondeur
class Address {
    String city;
    String zipCode;

    public Address(String city, String zipCode) {
        this.city = city;
        this.zipCode = zipCode;
    }
}
