package org.umcspro.genealogy;


import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Zadanie 1.
// Napisz klasę Person, w której znajdować będą się dane odpowiadające wierszowi pliku.
// Na tym etapie pomiń wczytywanie rodziców. Napisz metodę wytwórczą fromCsvLine()
// klasy Person przyjmującą jako argument linię opisanego pliku.
public class Person implements Serializable{
    private final String name;
    private final LocalDate birthDate;
    private final LocalDate deathDate;
    private final List<Person> parents;

    public Person(String name, LocalDate birthDate, LocalDate deathDate) {
        this.name = name;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.parents = new ArrayList<>();
    }

    public static Person fromCsvLine(String csvLine){
        String[] line= csvLine.split(",",-1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        LocalDate birthDate = LocalDate.parse(line[1],formatter);
        LocalDate deathDate = line[2].equals("")?null : LocalDate.parse(line[2],formatter);
        return new Person(line[0], birthDate, deathDate);
    }
    //Zadanie 2.
    //Napisz metodę fromCsv(), która przyjmie ścieżkę do pliku i zwróci listę obiektów typu Person.
    public static List<Person> fromCsv(String path) {
        List<Person> people = new ArrayList<>();
        //<----------------------------------------------------------------zad5

        Map<String, PersonWithParentsNames> mapPersonWithParentNames = new HashMap<>();
        //<----------------------------------------------------------------zad5

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                // <---------------------------------zad2
                //Person person = Person.fromCsvLine(line);
                   // person.validateLifeSpan();
                   // person.validateAmbiguous(people);

                //people.add(person);
                // <---------------------------------zad2
                //<---------------------------------zad5
                PersonWithParentsNames personWithNames = PersonWithParentsNames.fromCsvLine(line);
                personWithNames.getPerson().validateLifeSpan();
                personWithNames.getPerson().validateAmbiguous(people);

                Person person = personWithNames.getPerson();
                people.add(person);
                mapPersonWithParentNames.put(person.name,personWithNames);
                //<-------------------------------- zad5

            }
            PersonWithParentsNames.linkRelatives(mapPersonWithParentNames);
            try {
                for(Person person: people) {
                    System.out.println("Sprwadzam");
                    person.validateParentingAge();
                }
            }
            catch(ParentingAgeException exception) {
                Scanner scanner = new Scanner(System.in);
                System.out.println(exception.getMessage());
                System.out.println("Please confirm [Y/N]:");
                String response = scanner.nextLine();
                if(!response.equals("Y") && !response.equals("y"))
                    people.remove(exception.person);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }catch (NegativeLifespanException | AmbiguousPersonException e) {
            System.err.println(e.getMessage());
        }

        return people;
    }

    private void validateAmbiguous(List<Person> people) throws AmbiguousPersonException {
        for (Person person : people) {
            if (person.getName().equals(getName())) {
                throw new AmbiguousPersonException(person);
            }
        }
    }

    private void validateLifeSpan() throws NegativeLifespanException{
        if (deathDate != null && deathDate.isBefore(birthDate)){
            throw new NegativeLifespanException(this);
        }
    }

    private void validateParentingAge() throws ParentingAgeException {
        for(Person parent: parents) {
            if (birthDate.isBefore(parent.birthDate.plusYears(15)) || (parent.deathDate != null && birthDate.isAfter(parent.deathDate)))
                throw new ParentingAgeException(this, parent);
        }
    }


    public String getName() {
        return name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public LocalDate getDeathDate() {
        return deathDate;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", birthDate=" + birthDate +
                ", deathDate=" + deathDate +
                ", parents=" + parents +
                '}';
    }

    public void addParent(Person person) {
        parents.add(person);
    }


    //<----------------------------------------------------------------------------------------zad7

    public static void toBinaryFile(List<Person> people, String filename) throws IOException {
        try (
                FileOutputStream fos = new FileOutputStream(filename);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
        ) {
            oos.writeObject(people);
        }
    }

    public static List<Person> fromBinaryFile(String filename) throws IOException, ClassNotFoundException {
        try (
                FileInputStream fis = new FileInputStream(filename);
                ObjectInputStream ois = new ObjectInputStream(fis);
        ) {
            return (List<Person>) ois.readObject();
        }
    }
    //<----------------------------------------------------------------------------------------zad7

    public String generateTree(){
        Function<Person,String> cleanPersonName = p -> p.getName().replaceAll(" ","");
        Function<Person, String> addObject = person -> String.format("object %s", cleanPersonName.apply(person));

        StringBuilder sb = new StringBuilder("@startuml\n");
        String pname = cleanPersonName.apply(this);
        sb.append(addObject.apply(this));

        if (!parents.isEmpty()) {
            String parentSections = parents.stream()
                    .map(parent ->"\n"+addObject.apply(parent)+"\n"+
                            cleanPersonName.apply(parent)+"<--"+pname )
                    .collect(Collectors.joining());
            sb.append(parentSections);
        }
        return sb.append("\n@enduml").toString();
    }

    public static String generateTree(List<Person> people, Function<String, String> postProcess, Predicate<Person> condition) {
        Function<Person,String> clean = p -> p.getName().replaceAll(" ","");
        Function<Person, String> addObject = person -> String.format("object %s\n", clean.apply(person));
        Function<Person, String> post = addObject.andThen(postProcess);
            String objects = people.stream()
                    .map(person -> condition.test(person)?post.apply(person):addObject.apply(person) )
                    .collect(Collectors.joining());

            String relationships = people.stream()
                    .flatMap(person ->
                        person.getParents().isEmpty() ?  Stream.empty():
                        person.getParents().stream()
                                .map(parent -> String.format("%s <-- %s\n",clean.apply(parent),
                                        clean.apply(person)
                                        ))
                    )
                    .collect(Collectors.joining());

            return String.format("@startuml\n%s%s@enduml", objects, relationships);
        }

    public static List<Person> filterByName(List<Person> people, String substring) {
        return people.stream()
                .filter(person -> person.getName().contains(substring))
                .collect(Collectors.toList());
    }
    public static List<Person> sortDeadByLifespan(List<Person> people) {
        Function<Person, Long> getLifespan = person
                -> person.deathDate.toEpochDay() - person.birthDate.toEpochDay();

        return people.stream()
                .filter(person -> person.deathDate != null)
                .sorted((o2, o1) -> Long.compare(getLifespan.apply(o1), getLifespan.apply(o2)))
                .toList();
    }

    public static Person findOldestLiving(List<Person> people) {
        return people.stream()
                .filter(person -> person.deathDate == null)
                .min(Comparator.comparing(Person::getBirthDate))
                .orElse(null);
    }

    private List<Person> getParents() {
        return this.parents;
    }

}
