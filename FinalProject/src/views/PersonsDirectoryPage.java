package views;

import domain.Application;
import domain.Validator;
import helpers.DateHelper;
import helpers.Encryption;
import helpers.TableHelpers;
import models.Person;
import models.tablemodels.PersonsTableModel;
import utils.Dialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;

import static domain.Application.sqlDateFormat;

public class PersonsDirectoryPage extends BaseFrame {
    private final int EDIT_COLUMN_NUMBER = 3;
    private final int DELETE_COLUMN_NUMBER = 4;
    private JPanel p;
    private JPanel mainPanel;
    private JLabel heading;
    private JButton addPersonButton;
    private JButton cancelEditButton;
    private JPanel addPersonPane;
    private JTextField firstName;
    private JTextField lastName;
    private JTextField dateOfBirth;
    private JTextField username;
    private JPasswordField password;

    private JTable people;
    private JTextArea validationText;
    private JTextField email;
    private JTextField phone;
    private JButton updateRolesButton;

    private boolean editMode;
    private int currentlyEditingEmployee;
    private String existingPassword;

    public PersonsDirectoryPage() {
        super();
        setContentPane(p);

        displayPeople();

        setupActions();

        setEditMode(false);
    }


    private void addPerson() {
        if (!validateFields()) return;


        var person = new Person(currentlyEditingEmployee, firstName.getText(), lastName.getText(), DateHelper.tryGetDate(dateOfBirth.getText(), "yyyy-MM-dd"), username.getText(), Encryption.hash(password.getText()), email.getText(), phone.getText());

        if (!editMode) {

            try {
                Application.Database.Persons.add(person);
                Dialog.show(person.getFullName() + " added successfully.");
                displayPeople();


            } catch (SQLException ex) {
                ex.printStackTrace();
            }

        } else {
            try {

                if (!existingPassword.equals(password.getText())) {
                    person.setPassword(Encryption.hash(password.getText()));
                }

                Application.Database.Persons.update(person);
                Dialog.show(person.getFullName() + " updated successfully.");
                displayPeople();

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        setEditMode(false);
    }

    private void setEditMode(boolean mode) {
        editMode = mode;
        cancelEditButton.setVisible(mode);
        existingPassword = "";

        if (mode) {
            addPersonButton.setText("Update Person");
            addPersonPane.setBorder(BorderFactory.createTitledBorder("Edit Person"));
        } else {
            addPersonButton.setText("Add Person");
            addPersonPane.setBorder(BorderFactory.createTitledBorder("Add Person"));

            for (var text : new JTextField[]{firstName, lastName, dateOfBirth, username, password, email, phone}) {
                text.setText("");
                text.setBorder(BorderFactory.createLineBorder(Color.black));
            }
        }
    }

    private boolean validateFields() {
        var validationMessages = new ArrayList<String>();
        if (!Validator.checkTextsBlank(new JTextField[]{firstName, lastName, dateOfBirth, username, password, email, phone}))
            validationMessages.add("Enter all the mandatory fields");

        if (DateHelper.tryGetDate(dateOfBirth.getText(), sqlDateFormat) == null) {
            validationMessages.add("Date must be in yyyy-mm-dd format");
        }

        if (password.getPassword().length < 8) {
            validationMessages.add("Password must be 8 characters at least");
        }

        var passwordValidation = Validator.validatePassword(password);
        if (!passwordValidation.equals("")) {
            validationMessages.add(passwordValidation);
        }


        if (validationMessages.size() == 0) {
            validationText.setText("");
            return true;
        }

        validationText.setText(String.join("\n", validationMessages));

        return false;
    }

    private void setupActions() {
        addPersonButton.addActionListener(e -> addPerson());
        cancelEditButton.addActionListener(e -> setEditMode(false));
        updateRolesButton.addActionListener(e -> updateRoles());

        people.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                super.mouseClicked(me);

                JTable target = (JTable) me.getSource();
                int row = target.getSelectedRow(); // selected row
                int column = target.getSelectedColumn(); // selected column

                if (me.getClickCount() == 2) {
                    System.out.println("double click");
                    int personId = Integer.parseInt(target.getModel().getValueAt(row, 0) + "");

                    String personName = target.getModel().getValueAt(row, 1) + "";

                    if (column == DELETE_COLUMN_NUMBER) {
                        System.out.println("Delete Clicked");

                        if (personId == 1) {
                            Dialog.error("Can't delete the admin user");
                            return;
                        } else if (personId == Application.getCurrentlyLoggedInPerson().getId()) {
                            Dialog.error("Can't delete yourself");
                            return;
                        }

                        int result = Dialog.confirm("Are you sure you want to delete " + personName + "?", "Delete Person", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                        System.out.println(result);

                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                Application.Database.Persons.delete(personId);
                                displayPeople();
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    } else if (column == EDIT_COLUMN_NUMBER) {
                        if (personId == 1) {
                            Dialog.error("Can't edit the admin user");
                            return;
                        }

                        setEditMode(true);
                        currentlyEditingEmployee = personId;
                        try {
                            Person person = Application.Database.Persons.getById(personId);
                            firstName.setText(person.getFirstName());
                            lastName.setText(person.getLastName());
                            dateOfBirth.setText(DateHelper.formatDate(person.getDateOfBirth(), "yyyy-MM-dd"));
                            username.setText(person.getUsername());
                            password.setText(person.getPassword());
                            existingPassword = person.getPassword();
                            email.setText(person.getEmail());
                            phone.setText(person.getPhone());
                        } catch (SQLException e) {
                            Dialog.error("Error getting person");
                        }

                    }

                }
            }
        });

    }

    private void updateRoles() {

    }

    private void displayPeople() {
        try {
            people.setModel(new PersonsTableModel().loadData(Application.Database.Persons.getAll()));
        } catch (SQLException e) {
            Dialog.error("Error getting people");
            return;
        }

        TableHelpers.centerColumn(people, 0);
        TableHelpers.centerColumn(people, 3);
        TableHelpers.centerColumn(people, 4);
    }
}