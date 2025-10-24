export default class RowObject{
    row;
    item;
    fieldNameOrder;
    editButton;
    deleteButton;

    constructor(item, fieldNameOrder){
        this.item = item;
        this.fieldNameOrder = fieldNameOrder;
        this.editButton = this.createButton("bi bi-pencil-fill"); //bootstrap edit icon
        this.deleteButton = this.createButton("bi bi-trash-fill"); //bootstrap delete icon
        this.initRow();
    }

    createButton(iconClass){
        const button = document.createElement("button");
        button.innerHTML = '<i class="' + iconClass + '"></i>';
        button.rowObject = this; //link back to RowObject
        button.mode = "edit";
        return button;
    }

    initRow(){
        this.row = document.createElement("tr");
        this.fieldNameOrder.forEach(fieldName => this.row.appendChild(this.createDataCell(this.item, fieldName)));
        this.row.appendChild(this.createButtonCell(this.editButton));
        this.row.appendChild(this.createButtonCell(this.deleteButton));
    }

    createDataCell(item, fieldName){
        const cell = document.createElement("td");
        cell.textContent = item[fieldName];
        cell.type = "field";
        cell.name = fieldName;
        return cell;
    }

    createButtonCell(button){
        const cell = document.createElement("td");
        cell.style.width = "2vw";
        cell.appendChild(button);
        cell.type = "button";
        return cell;
    }
}