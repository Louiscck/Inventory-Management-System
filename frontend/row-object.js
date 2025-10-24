export default class RowObject{
    row;
    item;
    fieldOrder;
    editButton;
    deleteButton;

    constructor(item, fieldOrder){
        this.item = item;
        this.fieldOrder = fieldOrder;
        this.editButton = this.createButton("bi bi-pencil-fill"); //bootstrap edit icon
        this.deleteButton = this.createButton("bi bi-trash-fill"); //bootstrap delete icon
        this.initRow();
    }

    createButton(iconClass){
        const button = document.createElement("button");
        button.innerHTML = '<i class="' + iconClass + '"></i>';
        button.rowObject = this; //link back to RowObject
        return button;
    }

    initRow(){
        this.row = document.createElement("tr");
        this.fieldOrder.forEach(field => this.row.appendChild(this.createDataCell(this.item[field])));
        this.row.appendChild(this.createButtonCell(this.editButton));
        this.row.appendChild(this.createButtonCell(this.deleteButton));
    }

    createDataCell(field){
        const cell = document.createElement("td");
        cell.textContent = field;
        return cell;
    }

    createButtonCell(button){
        const cell = document.createElement("td");
        cell.style.width = "2vw";
        cell.appendChild(button);
        return cell;
    }
}