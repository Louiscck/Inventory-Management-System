package org.example;

@Table(name = "items")
public class Item {
    @PrimaryKey
    @Column(name = "id")
    private int id;
    @Column(name = "item_name")
    private String name;
    @Column(name = "category")
    private String category;
    @Column(name = "specification")
    private String specification;
    @Column(name = "unit")
    private String unit;
    @Column(name = "amount")
    private int amount;

    public Item (String name, String category, String specification, String unit, int amount){
        this.name = name;
        this.category = category;
        this.specification = specification;
        this.unit = unit;
        this.amount = amount;
    }

    public Item(){

    }

    public void printInfo(){
        System.out.println("Name = " + this.name +
                ", Category = " + this.category +
                ", Spec = " + this.specification +
                ", Unit = " + this.unit +
                ", Amount = " + this.amount
        );
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }


}
