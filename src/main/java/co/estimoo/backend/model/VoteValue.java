package co.estimoo.backend.model;

public enum VoteValue {
    ZERO("0"),
    ONE("1"),
    TWO("2"),
    THREE("3"),
    FIVE("5"),
    EIGHT("8"),
    THIRTEEN("13"),
    TWENTY_ONE("21"),
    QUESTION("?"),
    COFFEE("☕");

    private final String label;

    VoteValue(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static VoteValue fromLabel(String input) {
        for (VoteValue value : values()) {
            if (value.label.equals(input)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid vote value: " + input);
    }
}