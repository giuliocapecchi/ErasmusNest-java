package it.unipi.erasmusnest.dbconnectors;
import it.unipi.erasmusnest.graphicmanagers.AlertDialogGraphicManager;
import it.unipi.erasmusnest.model.Apartment;
import it.unipi.erasmusnest.model.Review;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.time.LocalDate;
import java.util.*;

import static org.neo4j.driver.Values.NULL;
import static org.neo4j.driver.Values.parameters;

public class Neo4jConnectionManager extends ConnectionManager implements AutoCloseable {
    ////////////////////////////////////// DRIVER OBJECT //////////////////////////////////////
    private final Driver driver;


    ////////////////////////////////////// CONNECTION URI //////////////////////////////////////
    private static final String PROTOCOL = "bolt://";
    private static final String NEO4J_HOST = "10.1.1.14";
    private static final String NEO4J_PORT = "7687";
    private static final String NEO4J_USER = "neo4j";
    private static final String NEO4J_PSW = "password";

    ////////////////////////////////////// CONSTRUCTOR //////////////////////////////////////
    public Neo4jConnectionManager()
    {
        super(NEO4J_HOST, Integer.parseInt(NEO4J_PORT));
        String connectionString = String.format("%s%s:%s", PROTOCOL, NEO4J_HOST, NEO4J_PORT);
        driver = GraphDatabase.driver(connectionString, AuthTokens.basic(NEO4J_USER, NEO4J_PSW));
    }

    @Override
    public void close() throws Exception {
        driver.close();
    }

    ////////////////////////////////////// CRUD OPERATION  //////////////////////////////////////

    //CREATE

    public boolean addApartment(Apartment apartment){
        try (Session session = driver.session()){
            session.writeTransaction((TransactionWork<Void>) tx -> {
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("apartmentId", apartment.getId());
                parameters.put("name", apartment.getName());
                parameters.put("pictureUrl", apartment.getImageURLs().get(0));
                parameters.put("averageReviewScore", apartment.getAverageRating());
                parameters.put("city", apartment.getCity());
                tx.run("MERGE (a:Apartment {apartmentId: $apartmentId, name: $name, pictureUrl: $pictureUrl, averageReviewScore: $averageReviewScore}) " +
                                "MERGE (c:City {name: $city}) " +
                                "MERGE (a)-[:LOCATED]->(c)",
                        parameters
                );

                return null;
            });
            return true;
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
        }
        return false;
    }

    public void addReview(Review review) {
        String email = review.getUserEmail();
        String apartmentId = review.getApartmentId();
        String comment = review.getComments();
        int score = review.getRating();
        LocalDate timestamp = review.getTimestamp();
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("email", email);
                parameters.put("apartmentId", apartmentId);
                parameters.put("comment", comment);
                parameters.put("score", score);
                parameters.put("timestamp", timestamp.toString());

                tx.run("MERGE (u:User {email: $email}) " +
                                 "WITH u "+
                                "MATCH (a:Apartment {apartmentId: $apartmentId}) " +
                                "MERGE (u)-[r:REVIEW]->(a) " +
                                "SET r.comment = $comment, r.score = $score, r.date = $timestamp " +
                                "RETURN r", parameters);
                return null;
            });
            updateApartmentAverageReviewScore(apartmentId);
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
        }
    }

    public boolean updateCitiesOfInterest(String email, ArrayList<String> city) {
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User {email: $email})-[r:INTERESTS]->(c:City) DELETE r",
                        parameters("email", email));
                for (String cityName : city) {
                    HashMap<String, Object> parameters = new HashMap<>();
                    parameters.put("email", email);
                    parameters.put("cityName", cityName);
                    tx.run("MERGE (u:User {email: $email}) " +
                                    "WITH u "+
                                    "MATCH (c:City {name: $cityName}) " +
                                    "MERGE (u)-[:INTERESTS]->(c)",
                            parameters);
                }
                return null;
            });
            return true;
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return false;
        }
    }



    //READ

    public List<Apartment> getApartmentsInCity(String cityName, int page, int elementsPerPage, int filter) {
        try (Session session = driver.session()) {
            int elementsToSkip =  (page-1)*elementsPerPage;
            List<Apartment> apartments = session.readTransaction((TransactionWork<List<Apartment>>) tx -> {
                Result result = null;

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("cityName", cityName);
                parameters.put("elementsToSkip",elementsToSkip);
                parameters.put("elementsPerPage", elementsPerPage);

                if(filter==0){ // default query without filter
                    result = tx.run("MATCH (a:Apartment)-[:LOCATED]->(c:City {name:$cityName}) " +
                                    "OPTIONAL MATCH (a)<-[r:REVIEW]-() " +
                                    "WITH a, COUNT(r) AS reviewCount " +
                                    "RETURN a, reviewCount " +
                                    "SKIP $elementsToSkip LIMIT $elementsPerPage", parameters);
                }else if(filter==1){ // query that orders the apartment based on the positiveness of the reviews
                    result = tx.run("MATCH (a:Apartment)-[:LOCATED]->(c:City {name:$cityName}) " +
                                    "OPTIONAL MATCH (a)<-[r:REVIEW]-() " +
                                    "WITH a, COUNT(r) AS reviewCount " +
                                    "RETURN a, reviewCount " +
                                    "ORDER BY CASE WHEN a.averageReviewScore IS NULL THEN 1 ELSE 0 END, a.averageReviewScore DESC " +
                                    "SKIP $elementsToSkip LIMIT $elementsPerPage", parameters);
                }else if(filter==2){ // query that orders the apartment based on the number of reviews
                    result = tx.run("MATCH (a:Apartment)-[:LOCATED]->(c:City {name:$cityName}) " +
                                        "OPTIONAL MATCH (a)<-[r:REVIEW]-() " +
                                        "WITH a, COUNT(r) AS reviewCount " +
                                        "RETURN a, reviewCount " +
                                        "ORDER BY CASE WHEN a.averageReviewScore IS NULL THEN 1 ELSE 0 END, reviewCount DESC " +
                                        "SKIP $elementsToSkip " +
                                        "LIMIT $elementsPerPage", parameters);
                }

                List<Apartment> apartmentList = new ArrayList<>();
                while (Objects.requireNonNull(result).hasNext()) {
                    Record record = result.next();
                    Node apartmentNode = record.get("a").asNode();
                    Integer reviewCount = record.get("reviewCount").asInt();
                    String apartmentId = apartmentNode.get("apartmentId").asString();
                    String apartmentName = apartmentNode.get("name").asString();
                    String pictureUrl = apartmentNode.get("pictureUrl").asString();
                    double averageReviewScore = 0.0;
                    Value propertyValue = apartmentNode.get("averageReviewScore");
                    if (propertyValue != NULL) {
                        averageReviewScore = propertyValue.asDouble();
                    }
                    Apartment apartment = new Apartment(apartmentId, apartmentName, pictureUrl, averageReviewScore, reviewCount);
                    apartmentList.add(apartment);
                }
                return apartmentList;
            });
            return apartments;
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }

    public List<String> getAllCities() {
        try (Session session = driver.session()) {
            return session.readTransaction((TransactionWork<List<String>>) tx -> {
                Result result = tx.run("MATCH (c:City) RETURN c.name");
                List<String> cityNamesList = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    String cityNameFromDB = record.get("c.name").asString();
                    cityNamesList.add(cityNameFromDB);
                }
                return cityNamesList;
            });
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }

    public Double getAverageReviewScore(String apartmentId) {
        try (Session session = driver.session()) {
            return session.readTransaction((TransactionWork<Double>) tx -> {
                Result result = tx.run("MATCH (a:Apartment {apartmentId: $apartmentId}) RETURN a.averageReviewScore",
                        parameters("apartmentId", apartmentId));
                if (result.hasNext()) {
                    return result.next().get("a.averageReviewScore").asDouble();
                } else {
                    return null;
                }
            });
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }


    public List<Review> getReviewsForApartment(String apartmentId, Integer page, Integer elementsPerPage, Integer filter) {
        try (Session session = driver.session()) {
            int elementsToSkip =  (page-1)*elementsPerPage;
            List<Review> reviews = session.readTransaction((TransactionWork<List<Review>>) tx -> {
                Result result = null;

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("apartmentId", apartmentId);
                parameters.put("elementsToSkip",elementsToSkip);
                parameters.put("elementsPerPage", elementsPerPage);

                if(filter==0){
                    result = tx.run("MATCH (u:User)-[r:REVIEW]->(a:Apartment {apartmentId: $apartmentId}) RETURN u.email,r "+
                                    "SKIP $elementsToSkip LIMIT $elementsPerPage", parameters);
                }else if(filter==1){
                    result = tx.run("MATCH (u:User)-[r:REVIEW]->(a:Apartment {apartmentId: $apartmentId}) " +
                                    "RETURN u.email,r " +
                                    "ORDER BY r.score DESC "+
                                    "SKIP $elementsToSkip LIMIT $elementsPerPage", parameters);

                }else if(filter==2){
                    result = tx.run("MATCH (u:User)-[r:REVIEW]->(a:Apartment {apartmentId: $apartmentId}) " +
                                    "RETURN u.email,r " +
                                    "ORDER BY r.score ASC "+
                                    "SKIP $elementsToSkip LIMIT $elementsPerPage", parameters);

                }else if(filter==3){ //ordino le recensioni per data (nuove per prime)
                    result = tx.run("MATCH (u:User)-[r:REVIEW]->(a:Apartment {apartmentId: $apartmentId}) " +
                                    "RETURN u.email,r " +
                                    "ORDER BY r.date DESC "+
                                    "SKIP $elementsToSkip LIMIT $elementsPerPage", parameters);
                }else if(filter==4){ //ordino le recensioni per data (più vecchie per prime)
                    result = tx.run("MATCH (u:User)-[r:REVIEW]->(a:Apartment {apartmentId: $apartmentId}) " +
                                    "RETURN u.email,r " +
                                    "ORDER BY r.date ASC "+
                                    "SKIP $elementsToSkip LIMIT $elementsPerPage", parameters);
                }
                if(result == null)
                    return null;
                List<Review> reviewList = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    String userEmail = record.get("u.email").asString();
                    Relationship reviewRel = record.get("r").asRelationship();
                    String comment = reviewRel.get("comment").asString();
                    int rating = reviewRel.get("score").asInt();
                    String timestamp = reviewRel.get("date").asString();
                    Review review = new Review(apartmentId,userEmail, comment,rating, timestamp);
                    reviewList.add(review);
                }
                return reviewList;
            });
            return reviews;
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }

    public List<Review> getReviewsForUser(String email, Integer page, Integer elementsPerPage) {
        try (Session session = driver.session()) {
            int elementsToSkip =  (page-1)*elementsPerPage;
            List<Review> reviews = session.readTransaction((TransactionWork<List<Review>>) tx -> {
                Result result;
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("email", email);
                parameters.put("elementsToSkip",elementsToSkip);
                parameters.put("elementsPerPage", elementsPerPage);

                System.out.println("email: " + email + " elementsToSkip: " + elementsToSkip + " elementsPerPage: " + elementsPerPage);

                result = tx.run("MATCH (u:User {email: $email})-[r:REVIEW]->(a:Apartment) " +
                        "RETURN r, a.apartmentId " +
                        "SKIP $elementsToSkip LIMIT $elementsPerPage;", parameters);
                List<Review> reviewList = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    Relationship reviewRel = record.get("r").asRelationship();
                    String comment = reviewRel.get("comment").asString();
                    int rating = reviewRel.get("score").asInt();
                    String timestamp = reviewRel.get("date").asString();
                    String apartmentId = record.get("a.apartmentId").asString();
                    Review review = new Review(apartmentId,email, comment,rating, timestamp);
                    reviewList.add(review);
                }
                return reviewList;
            });
            return reviews;
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }

    public Review getReview(String email, String apartmentId) {
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                HashMap<String, Object> parameters = new HashMap<>();
                parameters.put("email", email);
                parameters.put("apartmentId", apartmentId);
                System.out.println("email: " + email + " apartmentId: " + apartmentId);
                Result result = tx.run("MATCH (u:User {email: $email})-[r:REVIEW]->(a:Apartment {apartmentId: $apartmentId}) RETURN r",
                        parameters);
                if (result.hasNext()) {
                    Record record = result.next();
                    Relationship reviewRel = record.get("r").asRelationship();
                    String comment = reviewRel.get("comment").asString();
                    int rating = reviewRel.get("score").asInt();
                    String timestamp = reviewRel.get("date").asString();

                    System.out.println("Review found: " + comment + " " + rating + " " + timestamp);

                    return new Review(apartmentId, email, comment, rating, timestamp);
                } else {
                    return null;
                }
            });
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }


    public ArrayList<String> getCitiesOfInterest(String email) {
        try (Session session = driver.session()) {
            return (ArrayList<String>) session.readTransaction((TransactionWork<List<String>>) tx -> {
                Result result = tx.run("MATCH (u:User {email: $email})-[:INTERESTS]->(c:City) RETURN c.name",
                        parameters("email", email));
                ArrayList<String> interests = new ArrayList<>();
                while (result.hasNext()) {
                    interests.add(result.next().get("c.name").asString());
                }
                return interests;
            });
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }


    //UPDATE
    public void updateApartmentAverageReviewScore(String apartmentId) {
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH ()-[r:REVIEW]->(a:Apartment {apartmentId:$apartmentId}) " +
                                "WITH a, AVG(r.score) AS averageScore " +
                                "SET a.averageReviewScore = ROUND(averageScore * 100) / 100",
                        parameters("apartmentId", apartmentId));
                System.out.println("sei dopo");
                return null;
            });
        }catch (Exception e){
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
        }
    }

    // Method to get list of email of people followed by a user
    public List<String> getFollowsEmail(String email)
    {
        List<String> followsEmail = new ArrayList<>();
        try (Session session = driver.session())
        {
            return session.readTransaction((TransactionWork<List<String>>) tx -> {

                Result result = tx.run("MATCH (u:User {email: $email})-[f:FOLLOWS]->(u2:User) RETURN u2.email",
                        parameters("email", email));
                while(result.hasNext())
                {
                    Record record = result.next();
                    String emailFromDB = record.get("u2.email").asString();
                    followsEmail.add(emailFromDB);
                }
                System.out.println("\n\n\n Follows email: " + followsEmail + "\n\n\n");
                return followsEmail;
            });
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }

    // Method to get list of email of people following a user
    public List<String> getFollowersEmail(String email)
    {
        List<String> followersEmail = new ArrayList<>();
        try (Session session = driver.session())
        {
            return session.readTransaction((TransactionWork<List<String>>) tx -> {
                Result result = tx.run("MATCH (u:User {email: $email})<-[f:FOLLOWS]-(u2:User) RETURN u2.email",
                        parameters("email", email));
                while(result.hasNext())
                {
                    Record record = result.next();
                    String emailFromDB = record.get("u2.email").asString();
                    followersEmail.add(emailFromDB);
                }
                System.out.println("\n\n\n Followers email: " + followersEmail + "\n\n\n");
                return followersEmail;
            });
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }

    public void addFollow(String email, String otherEmail) {
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                HashMap<String, Object> parameters = new HashMap<>();
                parameters.put("email", email);
                parameters.put("otherEmail", otherEmail);
                tx.run("MERGE (u:User {email: $email}) " +
                                "MERGE (u2:User {email: $otherEmail}) " +
                                "MERGE (u)-[:FOLLOWS]->(u2)",
                        parameters);
                return null;
            });
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
        }
    }

    public void removeFollow(String email, String otherEmail) {
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User {email: $email})-[f:FOLLOWS]->(u2:User {email: $otherEmail}) " +
                                "DELETE f",
                        parameters("email", email, "otherEmail", otherEmail));
                return null;
            });
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
        }
    }

    public boolean removeApartment(String apartmentId) {
        try (Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (a:Apartment {apartmentId: $apartmentId}) " +
                                "DETACH DELETE a",
                        parameters("apartmentId", apartmentId));
                return null;
            });
            return true;
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return false;
        }
    }

    // Method to update apartment information
    public boolean updateApartment(String apartmentId, String pictureUrl)
    {
        try (Session session = driver.session())
        {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (a:Apartment {apartmentId: $apartmentId}) " +
                                "SET a.pictureUrl = $pictureUrl",
                        parameters("apartmentId", apartmentId, "pictureUrl", pictureUrl));
                return null;
            });
            return true;
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return false;
        }
    }

    public List<String> seeSuggestedUsers(String emailA, String emailB) {
        try (Session session = driver.session()) {
            return session.readTransaction((TransactionWork<List<String>>) tx -> {
                HashMap<String, Object> parameters = new HashMap<>();
                parameters.put("emailA", emailA);
                parameters.put("emailB", emailB);

                Result result = tx.run("MATCH (a:User {email: $emailA})-[:INTERESTS]->(city:City)<-[:INTERESTS]-(suggested:User), " +
                                    "(b:User {email: $emailB})-[:FOLLOWS]->(suggested) "+
                                    "WHERE NOT (a)-[:FOLLOWS]->(suggested) "+
                                    "AND suggested.email <> $emailA "+
                                    "RETURN DISTINCT suggested.email AS suggestedEmail",
                                    parameters);
                List<String> suggestedEmails = new ArrayList<>();
                while (result.hasNext()) {
                    suggestedEmails.add(result.next().get("suggestedEmail").asString());
                }
                return suggestedEmails;
            });
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            // Gestisci l'errore come preferisci o ritorna una lista vuota
            return Collections.emptyList();
        }
    }

    public boolean likeApartment(String id, String email) {
        boolean result = false;
        if (!likeExists(id, email)) {
            try (Session session = driver.session()) {
                session.writeTransaction((TransactionWork<Void>) tx -> {
                    tx.run(
                            "MERGE (u:User {email: $email}) " +
                                    "WITH u "+
                                    "MATCH (a:Apartment {apartmentId: $id})-[:LOCATED]->(c:City) " +
                                    "MERGE (u)-[:LIKES]->(a)" +
                                    "MERGE (u)-[:INTERESTS]->(c)",
                            parameters("email", email, "id", id));
                    return null;
                });
                result = true;
            } catch (Exception e) {
                new AlertDialogGraphicManager("Neo4j connection failed").show();
            }
        }
        return result;
    }

    private boolean likeExists(String id, String email) {
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                HashMap<String, Object> parameters = new HashMap<>();
                parameters.put("email", email);
                parameters.put("id", id);
                Result result = tx.run("MATCH (u:User {email: $email})-[l:LIKES]->(a:Apartment {apartmentId: $id}) RETURN l",
                        parameters);
                return result.hasNext();
            });
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return false;
        }
    }

    public ArrayList<Apartment> getFavourites(String email) {

        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                Result result = tx.run("MATCH (u:User {email: $email})-[l:LIKES]->(a:Apartment) RETURN a", parameters("email", email));
                ArrayList<Apartment> favourites = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    Node apartmentNode = record.get("a").asNode();
                    String apartmentId = apartmentNode.get("apartmentId").asString();
                    String name = apartmentNode.get("name").asString();
                    String url = apartmentNode.get("pictureUrl").asString();
                    double averageReviewScore = apartmentNode.get("averageReviewScore").asDouble();
                    favourites.add(new Apartment(apartmentId, name, url, averageReviewScore));
                }
                return favourites;
            });
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            return null;
        }
    }

    public void removeFavourite(String email, String favourite) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run("MATCH (u:User {email: $email})-[l:LIKES]->(a:Apartment {apartmentId: $favourite}) DELETE l",
                        parameters("email", email, "favourite", favourite));
                return null;
            });
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
        }
    }

    // Method to get suggested apartments
    public List<Apartment> getSuggestedApartments(String email, String cityName) {
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                Result result = tx.run(
                        "MATCH (u:User {email: $email})-[:FOLLOWS]->(f:User)-[r:REVIEW]->(a:Apartment)-[:LOCATED]->(c:City {name: $cityName}) " +
                                "WHERE r.score >= 3 " +
                                "WITH a, MAX(r.score) AS maxScore " +
                                "RETURN a.apartmentId AS apartmentId, a.name AS name, a.pictureUrl AS pictureUrl, a.averageReviewScore AS averageReviewScore " +
                                "ORDER BY maxScore DESC " +
                                "LIMIT 3",
                        parameters("email", email, "cityName", cityName));

                List<Apartment> suggestedApartments = new ArrayList<>();
                while (result.hasNext()) {
                    Record record = result.next();
                    String apartmentId = record.get("apartmentId").asString();
                    String name = record.get("name").asString();
                    String pictureUrl = record.get("pictureUrl").asString();
                    double averageReviewScore = record.get("averageReviewScore").asDouble();
                    suggestedApartments.add(new Apartment(apartmentId, name, pictureUrl, averageReviewScore));
                }
                return suggestedApartments;
            });
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
            // Gestisci l'errore come preferisci o ritorna una lista vuota
            return Collections.emptyList();
        }
    }

    public void removeReview(Review review) {
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                tx.run("MATCH (u:User {email: $email})-[r:REVIEW]->(a:Apartment {apartmentId: $apartmentId}) " +
                                "WHERE r.comment = $comment AND r.score = $score AND r.date = $timestamp " +
                                "DETACH DELETE r",
                        parameters("email", review.getUserEmail(), "apartmentId", review.getApartmentId(), "comment", review.getComments(), "score", review.getRating(), "timestamp", review.getTimestamp().toString()));
                return null;
            });
            updateApartmentAverageReviewScore(review.getApartmentId());
        } catch (Exception e) {
            System.out.println("Exception: " + e);
            new AlertDialogGraphicManager("Neo4j connection failed").show();
        }
    }
}



