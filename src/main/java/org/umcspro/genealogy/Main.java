package org.umcspro.genealogy;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {


        List<Person> people = Person.fromCsv("family.csv");
//        for(Person person: people)
//            System.out.println(person.generateTree());
//        people.forEach(System.out::println);


        people.stream()
                .map(person -> person.getName());
        //System.out.println(Person.generateUMLDiagram(people));
        PlantUMLRunner.setPlantUMLPath("./plantuml-1.2024.4.jar");
        //PlantUMLRunner.generateDiagram(Person.generateUMLDiagram(people),"./","outUML");

//        List<Person> filteredPeople = filterByName(people, "owal");
//        System.out.println("People whose names contain '" + "owal" + "':");
//        filteredPeople.forEach(System.out::println);

        List<Person> deadPeople = Person.sortDeadByLifespan(people);
        deadPeople.stream().map(person->person.getBirthDate().toEpochDay()-person.getDeathDate().toEpochDay() )
                .collect(Collectors.toList()).forEach(System.out::println);

        Function<String, String> colorYellow = s -> s.contains("object")?s.trim()+" #Yellow \n":s;
        Function<String, String> noChange = Function.identity();

        //Person.generateUMLDiagram(people, colorYellow);
        //Person.generateUMLDiagram(people, noChange);
        //PlantUMLRunner.generateDiagram(Person.generateUMLDiagram(people, colorYellow),"./","outUML");
        Predicate<Person> hasNameStartingWithA = person -> person.getName().contains("Kowalsk");

        PlantUMLRunner.generateDiagram(Person.generateTree(people, colorYellow, hasNameStartingWithA),"./","outUML");

//        try {
//            Person.toBinaryFile(people, "family.bin");
//            List<Person> loadPeople = Person.fromBinaryFile("family.bin");
//            for(Person person: loadPeople)
//                System.out.println(person);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (ClassNotFoundException e) {
//            throw new RuntimeException(e);
//        }

    }
}