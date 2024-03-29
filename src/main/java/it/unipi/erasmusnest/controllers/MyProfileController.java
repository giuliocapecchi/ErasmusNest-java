package it.unipi.erasmusnest.controllers;

import it.unipi.erasmusnest.consistency.RedisMongoConsistencyManager;
import it.unipi.erasmusnest.model.Apartment;
import it.unipi.erasmusnest.model.User;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.controlsfx.control.PopOver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MyProfileController extends Controller {

    @FXML
    private Button updateCitiesOfInterestButton;
    @FXML
    private VBox personalInfoVbox;
    @FXML
    private VBox citiesVBox;
    @FXML
    private TitledPane cityTitlePane;
    @FXML
    private VBox apartmentsContainerVBox;
    @FXML
    private VBox apartmentsContainer;
    @FXML
    private Label emailLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private Label lastNameLabel;
    @FXML
    private PasswordField passwordField; // Campo password
    @FXML
    private Button modifyPasswordButton; // Bottone per la modifica della password
    @FXML
    private VBox passwordChangeBox; // Banner/pop-up per la modifica della password
    @FXML
    private VBox passwordChangeOuterBox;
    @FXML
    private PasswordField newPasswordField; // Campo per la nuova password
    @FXML
    private PasswordField confirmNewPasswordField; // Campo per la conferma della nuova password
    @FXML
    private Label passwordErrorLabel; // Etichetta per visualizzare gli errori
    @FXML
    private ComboBox<String> studyFieldComboBox;
    @FXML
    private VBox favouritesContainerVBox;
    boolean isEditingPassword = false; // Aggiunto per gestire la modifica della password
    @FXML
    private Button buttonPwdUpdate;
    @FXML
    private Button adminButton;
    @FXML
    private TitledPane incomingReservationPane;
    @FXML
    private TitledPane favouriteAptsPane;
    @FXML
    private Accordion accordion;
    @FXML
    private TitledPane infoPane;

    private String selectedStudyField;
    private ArrayList<String> citiesOfInterestInNeo4j = new ArrayList<>();
    private final ArrayList<String> selectedCitiesOfInterest = new ArrayList<>();



    // Metodo per impostare il testo dell'etichetta dell'errore
    private void setPasswordErrorText() {
        passwordErrorLabel.setText("The passwords don't match or the new password is invalid.");
    }

    public MyProfileController() {}

    @FXML
    private void initialize() {

        getSession().setCity(null); // serve per la change window
        personalInfoVbox.prefWidthProperty().bind(super.getRootPane().widthProperty().multiply(0.4));
        apartmentsContainerVBox.prefWidthProperty().bind(super.getRootPane().widthProperty().multiply(0.4));
        passwordChangeOuterBox.prefWidthProperty().bind(super.getRootPane().widthProperty());
        updateCitiesOfInterestButton.setDisable(true); // viene abilitato solo se l'utente effettivamente modifica le città di interesse

        String userEmail = getSession().getUser().getEmail();

        User utente = getMongoConnectionManager().findUser(userEmail);
        // Set the password field with asterisks, one for each character
        passwordField.setText("*".repeat(getSession().getUser().getPassword().length()));

        // Nascondi il banner/pop-up per la modifica della password all'inizio
        passwordChangeBox.setVisible(false);
        passwordChangeOuterBox.setVisible(false);
        passwordChangeOuterBox.getChildren().clear();

        // Inizializza il ComboBox per il campo "Study Field" (SF)

        studyFieldComboBox.getItems().addAll(getSession().getStudyFieldsOptions());
        // Inizializza il ComboBox per il campo "Cities of Interest" (CoI)
        // Estrai il campo "Study Field" (SF) e "Cities of Interest" (CoI) dal documento dell'utente
        //selectedStudyField = userDocument.getString("SF");
        selectedStudyField = utente.getStudyField()==null ? "" : utente.getStudyField();

        // Imposta i valori iniziali nei ComboBox
        studyFieldComboBox.setValue(selectedStudyField);

        nameLabel.setText(utente.getName());
        lastNameLabel.setText(utente.getSurname());
        emailLabel.setText(utente.getEmail());

        if(utente.getApartments() != null  && !utente.getApartments().isEmpty()){
            // Recupera gli appartamenti dell'utente e li aggiunge al VBox apartmentsContainer
            for (Apartment apartment : utente.getApartments())
            {

                HBox apartmentBox = new HBox(10);
                apartmentBox.setAlignment(Pos.CENTER_LEFT);

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
                apartmentButton.setText("Modify");

                apartmentButton.setId(apartment.getId());
                //Now add the apartment image and button to the HBox
                apartmentButton.setOnAction(event -> {
                    // Chiamare il metodo desiderato quando il bottone viene premuto
                    onApartmentView(apartmentButton.getId());
                });
                Button viewButton = new Button(apartment.getName());
                viewButton.setOnAction(event -> onChangeView(apartmentButton.getId()));
                apartmentBox.getChildren().addAll(apartmentImage, viewButton, apartmentButton);
                apartmentsContainer.getChildren().add(apartmentBox); // This should add the apartment to the UI
            }
        }else{
            accordion.getPanes().remove(incomingReservationPane);
        }
        // Parte riservata all'ADMIN
        // Se sono admin, allora appare un bottone per accedere alla vista analitiche
        if (utente.isAdmin()){
            adminButton.setVisible(true);
        }

        favouriteAptsPane.expandedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue){
                onFavouritesButtonClick();
            }
        });

        utente.setPassword(getSession().getUser().getPassword());
        getSession().setUser(utente);
    }

    private void onChangeView(String apartmentId){
        if(getSession().getApartment() == null)
            getSession().setApartment(new Apartment());
        getSession().getApartment().setId(apartmentId);
        super.changeWindow("apartment");
    }

    public TitledPane createTitledPane(List<String> userCities){
        GridPane gridPane = new GridPane();
        selectedCitiesOfInterest.addAll(citiesOfInterestInNeo4j); // selected cities of interest lo uso per gestire quando abilitare il tasto di upload
        System.out.println("cities of interest in neo4j: "+citiesOfInterestInNeo4j);
        System.out.println("selected cities of interest: "+selectedCitiesOfInterest);

        for(String city : getSession().getCities()){
            CheckBox mainCheckBox = new CheckBox(city);
            mainCheckBox.setText(city);
            mainCheckBox.setOnAction(e -> {
                if (mainCheckBox.isSelected()) {
                    selectedCitiesOfInterest.add(mainCheckBox.getText());
                    updateCitiesOfInterestButton.setDisable(selectedCitiesOfInterest.equals(citiesOfInterestInNeo4j));
                }else{
                    selectedCitiesOfInterest.remove(mainCheckBox.getText());
                    updateCitiesOfInterestButton.setDisable(selectedCitiesOfInterest.equals(citiesOfInterestInNeo4j));
                }

                System.out.println("cities of interest in neo4j: "+citiesOfInterestInNeo4j);
                System.out.println("selected cities of interest: "+selectedCitiesOfInterest);
            });
            if(userCities!=null && userCities.contains(city)){
                mainCheckBox.setSelected(true);
            }
            gridPane.add(mainCheckBox, 0, getSession().getCities().indexOf(city));
        }
        TitledPane titledPane = new TitledPane("Update cities of interest", gridPane);
        titledPane.setAnimated(false);
        titledPane.setExpanded(true);
        return titledPane;
    }

    @FXML
    protected void logoutButtonClick(){
        setFirstWindow("login");
        getSession().reset();
        super.changeWindow("login");
    }

    @FXML
    private void onBackButtonClick() {
        passwordChangeOuterBox.setVisible(false);
        newPasswordField.clear();
        confirmNewPasswordField.clear();
        onModifyPasswordButtonClick();
    }

    @FXML
    private void onModifyPasswordButtonClick() {
        // Mostra il banner/pop-up per la modifica della password
        if (isEditingPassword) {
            passwordChangeOuterBox.setVisible(false);
            passwordChangeOuterBox.getChildren().clear();
            isEditingPassword = false;
        }
        else
        {
            // Mostra il banner/pop-up per la modifica della password
            passwordChangeBox.setVisible(true);
            passwordChangeOuterBox.setVisible(true);
            if(!passwordChangeOuterBox.getChildren().contains(passwordChangeBox))
            {
                passwordChangeOuterBox.getChildren().add(passwordChangeBox);
            }
            isEditingPassword = true;
        }
    }

    @FXML
    private void onUpdatePasswordClick() {
        String newPassword = newPasswordField.getText();
        String confirmNewPassword = confirmNewPasswordField.getText();

        // Aggiungi la logica per verificare la vecchia password
        // String currentPassword = getCurrentUserPassword(); // Sostituisci con la tua logica per ottenere la password corrente
        if (newPassword.length() >= 4 && newPassword.length() <= 20 && newPassword.equals(confirmNewPassword))
        {
            if(getRedisConnectionManager().updateUserPassword(getSession().getUser().getEmail(), newPassword)){
                System.out.println("Password aggiornata su Redis");
                passwordChangeOuterBox.setVisible(false);
                passwordField.setText("*".repeat(newPassword.length()));
                showConfirmationMessage("Password aggiornata con successo!", modifyPasswordButton);
                onModifyPasswordButtonClick();

                new RedisMongoConsistencyManager(getRedisConnectionManager(), getMongoConnectionManager()).updateUserPasswordOnMongo(getSession().getUser().getEmail(), newPassword, getSession().getReservationsApartmentIds());

            }else{ // aggiorno su Mongo e basta
                System.out.println("Password non aggiornata su Redis perchè non trovata la chiave. La aggiorno su MongoDB");
                // per come è ora il codice, su mongo la password viene aggiornata a priori (errore???)
                if(getMongoConnectionManager().updatePassword(getSession().getUser().getEmail(), newPassword)){
                    // Nascondi il banner/pop-up e mostra un messaggio di conferma
                    //passwordChangeBox.setVisible(false);
                    passwordChangeOuterBox.setVisible(false);
                    showConfirmationMessage("Password aggiornata con successo!", modifyPasswordButton);
                }else{
                    // Mostra un messaggio di errore se la nuova password non è valida o la vecchia password non coincide
                    setPasswordErrorText();
                }
            }
        }
        else
        {
            // Mostra un messaggio di errore se la nuova password non è valida o la vecchia password non coincide
            setPasswordErrorText();
        }
    }

    // Metodo per mostrare un messaggio di conferma
    private void showConfirmationMessage(String message, Node node) {
        PopOver popOver = new PopOver();
        Label label = new Label(message);
        label.setStyle("-fx-padding: 10px;");
        popOver.setContentNode(label);
        popOver.setDetachable(false);
        popOver.setAutoHide(true);
        popOver.show(node);
    }

    @FXML
    private void onStudyFieldSelectionChanged() {
        String newStudyField = studyFieldComboBox.getValue();
        if (!newStudyField.equals(selectedStudyField))
        {
            if(getMongoConnectionManager().updateStudyField(getSession().getUser().getEmail(),newStudyField))
            {
                // Aggiorna la variabile con il nuovo valore
                selectedStudyField = newStudyField;
                showConfirmationMessage("Study Field aggiornato con successo!", studyFieldComboBox);
            }
            else
            {
                showConfirmationMessage("Errore nell'aggiornamento di study field!", studyFieldComboBox);
            }
        }
    }

    public void getCitiesOfInterest() {
        citiesOfInterestInNeo4j = getNeo4jConnectionManager().getCitiesOfInterest(getSession().getUser().getEmail());
        cityTitlePane = createTitledPane(citiesOfInterestInNeo4j);
        citiesVBox.getChildren().clear();
        citiesVBox.getChildren().add(cityTitlePane);
    }

    public void onUploadApartmentButtonClick() {
        super.changeWindow("uploadApartment");
    }

    public void onApartmentView(String apartmentId) {
        getSession().getApartment().setId(apartmentId);
        super.changeWindow("modifyApartment");
    }

    public void onBack() {
        super.changeWindow("login");
    }

    @FXML
    protected void onReservationsButtonClick() {
        getSession().setMyApartmentsIds(null);
        super.changeWindow("myreservations");
    }

    @FXML
    protected void onReservationsMyApartmentsButtonClick() {
        // mettere in sessione gli id di tutte le case
        ArrayList<String> ids = new ArrayList<>();
        for(Apartment apartment : getSession().getUser().getApartments()){
            ids.add(apartment.getId());
        }
        getSession().setMyApartmentsIds(ids);
        super.changeWindow("myreservations");
    }

    public void onFollowersButtonClick() {
        super.changeWindow("followers");
    }

    public void onFavouritesButtonClick() {
        if(favouriteAptsPane.isExpanded()) {

            favouritesContainerVBox.getChildren().clear();
            System.out.println("Favourites button clicked");
            ArrayList<Apartment> favourites = getNeo4jConnectionManager().getFavourites(getSession().getUser().getEmail());

            if (favourites == null || favourites.isEmpty()) {
                Label favouritesLabel = new Label();
                favouritesLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #000000;");
                favouritesContainerVBox.getChildren().add(favouritesLabel);
                favouritesLabel.setText("No favourites apartment found.");
            } else {
                for (Apartment apartment : favourites) {
                    Button button = new Button();
                    button.setText(apartment.getName());
                    button.setOnAction(event -> {
                        getSession().getApartment().setId(apartment.getId());
                        super.changeWindow("apartment");
                    });
                    Button dislike = new Button("dislike");
                    dislike.setOnAction(event -> {
                        getNeo4jConnectionManager().removeFavourite(getSession().getUser().getEmail(), apartment.getId());
                        super.changeWindow("myprofile");
                    });

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

                    HBox hBox = new HBox(10);
                    hBox.setAlignment(Pos.CENTER_LEFT);
                    apartmentImage.fitWidthProperty().bind(hBox.widthProperty().multiply(0.4));
                    hBox.getChildren().addAll(apartmentImage, button, dislike);
                    favouritesContainerVBox.getChildren().addAll(hBox);
                }
            }
        }
    }

    @FXML
    protected void checkNewPassword() {
        if (isTextFieldValid(newPasswordField) && isTextFieldValid(confirmNewPasswordField)) {
            buttonPwdUpdate.setDisable(!newPasswordField.getText().equals(confirmNewPasswordField.getText()));
        } else {
            buttonPwdUpdate.setDisable(true);
        }
    }

    public void onUpdateCitiesOfInterestButtonClick() {
        if(getNeo4jConnectionManager().updateCitiesOfInterest(getSession().getUser().getEmail(), selectedCitiesOfInterest)){
            citiesOfInterestInNeo4j.clear();
            citiesOfInterestInNeo4j.addAll(selectedCitiesOfInterest);
            showConfirmationMessage("Città di interesse aggiornate con successo!", updateCitiesOfInterestButton);
            updateCitiesOfInterestButton.setDisable(true);
            cityTitlePane.setExpanded(false);
        }else{
            showConfirmationMessage("Errore nell'aggiornamento delle città di interesse!", updateCitiesOfInterestButton);
        }
    }

    @FXML
    public void onAdminView(){
        super.changeWindow("analytics");
    }

}
