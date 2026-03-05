public class UserData {
    private String username;
    private String password;
    private String name;
    private int age;
    private String address;
    private String email;
    private String phoneNumber;

    public UserData(String username, String password, String name, int age, String address, String email, String phoneNumber) {
        this.username = username;
        this.password = password;
        this.name = name;
        this.age = age;
        this.address = address;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public String getAddress() {
        return address;
    }

    public String getEmail() {
        return email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    @Override
    public String toString() {
        return String.join(",", username, password, name, String.valueOf(age), address, email, phoneNumber);
    }
}