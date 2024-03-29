package it.unipi.erasmusnest.model;

import javafx.geometry.Point2D;

import java.util.ArrayList;

public class Apartment {

    private String id;              // embedded in User (host)
    private String name;            // embedded in User (host)
    private String description;
    private Point2D location;
    private String city;
    private Integer dollarPriceMonth;
    private Integer maxAccommodates;
    private String hostEmail;
    private ArrayList<String> imageUrls;        // the first is also embedded in User (host)
    private Double averageRating;    // in Neo4j
    private int numberOfReviews = 0; // in neo4j
    private Integer bathrooms; // in MONGO
    private String hostName;
    private String hostSurname;


    public Apartment(String id, String houseName, String imageURL, Double averageReviewScores, Integer numberOfReviews) {
        this.id = id;
        this.name = houseName;
        this.imageUrls = new ArrayList<>();
        this.imageUrls.add(imageURL);
        this.averageRating = averageReviewScores;
        this.numberOfReviews = numberOfReviews;
    }

    public Apartment(String id, String houseName, String description, Point2D point2D, Integer price, Integer accommodates, String hostEmail, ArrayList<String> imageUrls, Integer bathroomsNumber)
    {
        this.id = id;
        this.name = houseName;
        this.description = description;
        this.location = point2D;
        this.dollarPriceMonth = price;
        this.maxAccommodates = accommodates;
        this.hostEmail = hostEmail;
        this.imageUrls = imageUrls;
        this.bathrooms = bathroomsNumber;
    }

    public Apartment(String houseName, String houseDescription, Point2D location, Integer price, Integer accommodates, String userEmail, ArrayList<String> arrayList, double v, int i, Integer bathrooms, String name, String surname) {
        this.name = houseName;
        this.description = houseDescription;
        this.location = location;
        this.dollarPriceMonth = price;
        this.maxAccommodates = accommodates;
        this.hostEmail = userEmail;
        this.imageUrls = arrayList;
        this.averageRating = v;
        this.numberOfReviews = i;
        this.bathrooms = bathrooms;
        this.hostName = name;
        this.hostSurname = surname;
    }


    public Apartment(String houseName, String houseDescription, Point2D location, String city, Integer price, Integer accommodates, String userEmail, ArrayList<String> pictureUrls, double averageRating, int numberOfReviews, Integer bathrooms, String name, String surname) {
        this.name = houseName;
        this.description = houseDescription;
        this.location = location;
        this.city = city;
        this.dollarPriceMonth = price;
        this.maxAccommodates = accommodates;
        this.hostEmail = userEmail;
        this.imageUrls = pictureUrls;
        this.averageRating = averageRating;
        this.numberOfReviews = numberOfReviews;
        this.bathrooms = bathrooms;
        this.hostName = name;
        this.hostSurname = surname;
    }

    public Apartment(String id, String houseName) {
        this.id = id;
        this.name = houseName;
    }

    public Apartment(String apartmentId, String name, String pictureUrl, double averageReviewScore) {
        this.id = apartmentId;
        this.name = name;
        this.imageUrls = new ArrayList<>();
        this.imageUrls.add(pictureUrl);
        this.averageRating = averageReviewScore;
    }

    public Apartment() {
    }

    public String getHostName() {
        return hostName;
    }

    public String getHostSurname() {
        return hostSurname;
    }

    public Integer getBathrooms() {
        return bathrooms;
    }

    public void setBathrooms(Integer bathrooms) {
        this.bathrooms = bathrooms;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Point2D getLocation() {
        return location;
    }

    public ArrayList<String> getImageURLs() {
        return imageUrls;
    }

    public void setImageURL(ArrayList<String> imageURL) {
        this.imageUrls=imageURL;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public String getHostEmail() {
        return hostEmail;
    }

    public Integer getDollarPriceMonth() {
        return dollarPriceMonth;
    }

    public Integer getMaxAccommodates() {
        return maxAccommodates;
    }

    public void setMaxAccommodates(Integer maxAccommodates) {
        this.maxAccommodates = maxAccommodates;
    }

    public void setDollarPriceMonth(Integer dollarPriceMonth) {
        this.dollarPriceMonth = dollarPriceMonth;
    }

    @Override
    public String toString() {
        return "Apartment{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", position=" + location +
                ", dollarPriceMonth=" + dollarPriceMonth +
                ", maxAccommodates=" + maxAccommodates +
                ", bathrooms=" + bathrooms +
                ", email='" + hostEmail + '\'' +
                ", imageURL='" + imageUrls + '\'' +
                ", averageRating=" + averageRating +
                '}';

    }

    public Object getNumberOfReviews() {
        return numberOfReviews;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
