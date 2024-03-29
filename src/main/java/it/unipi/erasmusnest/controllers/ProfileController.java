package it.unipi.erasmusnest.controllers;

import it.unipi.erasmusnest.model.Apartment;
import it.unipi.erasmusnest.model.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;

import java.util.List;
import java.util.Objects;

public class ProfileController extends Controller{

    @FXML
    private ToolBar toolBar;
    @FXML
    private Button backToSearch;
    @FXML
    private Label pageTitle;
    @FXML
    private Label emailLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private Label surnameLabel;
    @FXML
    private Label studyFieldLabel;
    @FXML
    public VBox apartmentsContainer;
    @FXML
    private VBox suggestedOuterBox;
    @FXML
    private VBox suggestedVBox;
    @FXML
    private Button followButton;
    @FXML
    private Button removeProfileButton;

    public ProfileController() {

    }


    @FXML
    private void initialize() {

        if (getSession().getOtherProfileMail() != null) {

            if(Objects.equals(getSession().getOtherProfileMail(), getSession().getUser().getEmail()))
                super.changeWindow("myprofile");

            User utente = getMongoConnectionManager().findUser(getSession().getOtherProfileMail());

            pageTitle.setText("Profile of " + utente.getName() + " " + utente.getSurname());

            removeProfileButton.setVisible(getSession().getUser().isAdmin());

            emailLabel.setText(utente.getEmail());
            nameLabel.setText(utente.getName());
            surnameLabel.setText(utente.getSurname());

            followButton.setDisable(!getSession().isLogged());
            suggestedOuterBox.prefWidthProperty().bind(super.getRootPane().widthProperty());
            suggestedOuterBox.setVisible(false);

            if(utente.getStudyField() == null ||utente.getStudyField().isEmpty() || utente.getStudyField().isBlank())
                studyFieldLabel.setText("not specified");
            else
                studyFieldLabel.setText(utente.getStudyField());

            //Adesso si deve popolare la vbox per le case dell'utente
            List<Apartment> userApartments = utente.getApartments();
            // Recupera gli appartamenti dell'utente e li aggiunge al VBox apartmentsContainer
            if (userApartments != null && !userApartments.isEmpty()) {
                for (Apartment apartment : userApartments) {

                    HBox apartmentBox = new HBox();

                    ImageView apartmentImage = new ImageView();
                    apartmentImage.setPreserveRatio(true);

                    String imageUrl = null;
                    if(apartment.getImageURLs()!=null && !apartment.getImageURLs().isEmpty()){
                        imageUrl = apartment.getImageURLs().get(0);
                    }
                    String noImageAvaialblePath = "/media/no_photo_available.png";
                    if(imageUrl==null || imageUrl.isEmpty()){
                        apartmentImage.setImage(new Image(Objects.requireNonNull(getClass().getResourceAsStream(noImageAvaialblePath))));
                    }else{
                        Image image;
                        try{
                            image = new Image(imageUrl,true); // può fallire se il link è non valido
                        }catch (Exception e) {
                            System.out.println("not valid URL for the image");
                            image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(noImageAvaialblePath)));
                        }
                        apartmentImage.setImage(image);
                    }

                    apartmentImage.setSmooth(true);
                    apartmentImage.fitWidthProperty().bind(apartmentBox.widthProperty().multiply(0.4));

                    Button apartmentButton = new Button();
                    apartmentButton.setText(apartment.getName());
                    apartmentButton.setOnAction(event -> {
                        // Handle the apartment button click
                        System.out.println("Apartment button clicked");
                        // Setto l'id dell'appartamento nella sessione
                        getSession().getApartment().setId(apartment.getId());
                        super.changeWindow("apartment");
                    });

                    //Now add the apartment image and button to the HBox
                    apartmentBox.getChildren().addAll(apartmentImage, apartmentButton);
                    apartmentsContainer.getChildren().add(apartmentBox); // This should add the apartment to the UI
                    if(Objects.equals(getPreviousWindowName(), "homepage")
                            || Objects.equals(getPreviousWindowName(), "profile")) {
                        toolBar.getItems().remove(backToSearch);
                    }
                }
            } else {
                apartmentsContainer.getChildren().clear();
            }
        }
    }


    @FXML
    protected void backToBrowse() {
        super.backToPreviousWindow();
    }

    @FXML
    protected void backToHomepage() {
        super.changeWindow("homepage");
    }

    @FXML
    protected void seeSuggested(ActionEvent actionEvent)
    {
        String otherEmail = getSession().getOtherProfileMail();
        String email = getSession().getUser().getEmail();
        if(!email.equals(otherEmail) ) {
            getNeo4jConnectionManager().addFollow(email, otherEmail);
            List<String> suggestedUsers =  getNeo4jConnectionManager().seeSuggestedUsers(email, otherEmail);
            // Show pop up with suggested users
            suggestedVBox.setVisible(true);
            suggestedOuterBox.setVisible(true);
            if (suggestedUsers != null && !suggestedUsers.isEmpty()) {
                for (String suggestedUser : suggestedUsers) {
                    Button suggestedUserButton = new Button(suggestedUser);
                    suggestedVBox.getChildren().add(suggestedUserButton);
                    suggestedUserButton.setOnAction(event -> {
                        getSession().setOtherProfileMail(suggestedUser);
                        super.refreshWindow();
                    });
                }
                showConfirmationMessage("Started follow", followButton);
                followButton.setDisable(true);
            } else {
                suggestedOuterBox.getChildren().addAll(new Label("No suggested users"));
                followButton.setDisable(true);
            }
        }
        else {
            showConfirmationMessage("You can't follow yourself", followButton);
            followButton.setDisable(true);
        }
    }

    @FXML
    protected void removeUser(ActionEvent actionEvent) {
        String otherEmail = getSession().getOtherProfileMail();
        String email = getSession().getUser().getEmail();
        if(!email.equals(otherEmail)) {
            getMongoConnectionManager().removeUser(email);
            super.changeWindow("login");
        }
    }

    private void showConfirmationMessage(String message, Button likeButton) {
        PopOver popOver = new PopOver();
        Label label = new Label(message);
        label.setStyle("-fx-padding: 10px;");
        popOver.setContentNode(label);
        popOver.setDetachable(false);
        popOver.setAutoHide(true);
        popOver.show(likeButton);
    }

    public void showReviews() {
        super.changeWindow("reviews");
    }
}
