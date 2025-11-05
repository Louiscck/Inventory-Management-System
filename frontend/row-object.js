export default class RowObject{
    row;
    item;
    fieldNameOrder;
    editButton;
    deleteButton;
    submitButton;
    cancelButton;
    serverUrl;

    constructor(item, fieldNameOrder, serverUrl){
        this.item = item;
        this.fieldNameOrder = fieldNameOrder;
        this.serverUrl = serverUrl;
        this.editButton = this.createButton("bi bi-pencil-fill", this.switchToEditMode); //bootstrap edit icon
        this.deleteButton = this.createButton("bi bi-trash-fill", this.deleteItem); //bootstrap delete icon
        this.submitButton = this.createButton("bi bi-check2-all", this.editItem); //bootstrap submit icon
        this.cancelButton = this.createButton("bi bi-x-lg", this.switchToDisplayMode); //bootstrap cancel icon
        this.initRow();
    }

    createButton(iconClass, func){
        const button = document.createElement("button");
        button.innerHTML = '<i class="' + iconClass + '"></i>';
        button.addEventListener("click", func.bind(this));
        return button;
    }

    initRow(){
        this.row = document.createElement("tr");
        this.fieldNameOrder.forEach(fieldName => this.row.appendChild(this.createDataCell(fieldName)));
        this.row.appendChild(this.createButtonCell(this.editButton));
        this.row.appendChild(this.createButtonCell(this.deleteButton));
    }

    createDataCell(fieldName){
        const cell = document.createElement("td");
        cell.textContent = this.item[fieldName];
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

    async deleteItem(){
        const confirmed = confirm("Are you sure you want to delete this item?");
        if(!confirmed){
            return;
        }

        const url = this.serverUrl + "/item/" + this.item.id;
        console.log("Deleting item with ID: " + this.item.id);
        this.addSpinner("delete-edit-item-text");
        let response;
        try{
            response = await fetch(url, {
                method: "DELETE",
            })
        } catch(error) {
            document.getElementById("delete-edit-item-text").innerText = "Error connecting to server.";
            return;
        }
        let responseData = null; //204 has no body, may cause error when calling response.json()
        if(response.status !== 204){
            responseData = await response.json();
        }
        this.printDeleteItemResponseMessage(response, responseData);
        if(response.status === 204){
            const table = document.querySelector("#display-item-table tbody");
            table.removeChild(this.row);
        }
    }

    printDeleteItemResponseMessage(response, responseData){
        const textObj = document.getElementById("delete-edit-item-text");
        textObj.innerText = "";
        switch(response.status){
            case 204:
                textObj.innerText = "Item deleted successfully!";
                break;
            case 400:
            case 404:
            case 500:
                textObj.innerText = responseData.errorMessage;
                break;
            default:
                textObj.innerText = "Unexpected error occurred.";
        }
    }

    switchToEditMode(){
        this.row.removeChild(this.editButton.parentElement);
        this.row.removeChild(this.deleteButton.parentElement);
        this.row.appendChild(this.createButtonCell(this.submitButton));
        this.row.appendChild(this.createButtonCell(this.cancelButton));
        this.addInputToCells();
    }

    addInputToCells(){
        const cells = this.row.querySelectorAll("td");
        cells.forEach(cell => {
            if(cell.type === "field"){
                cell.textContent = "";
                const value = this.item[cell.name];
                const input = document.createElement("input");
                input.type = "text";
                input.value = value;
                input.style.width = "100%";
                if(cell.name === "amount"){
                    input.type = "number";
                    input.min = "0";
                    input.addEventListener("input", function(){
                        this.value = Math.max(0,this.value);
                    });
                }
                input.addEventListener("keydown", (event) => {
                    if(event.key === "Enter"){
                        this.editItem();
                    }
                });
                cell.appendChild(input);
            }
        });
    }

    switchToDisplayMode(){
        this.row.removeChild(this.submitButton.parentElement);
        this.row.removeChild(this.cancelButton.parentElement);
        this.row.appendChild(this.createButtonCell(this.editButton));
        this.row.appendChild(this.createButtonCell(this.deleteButton));
        this.restoreCellsDisplay();
    }

    restoreCellsDisplay(){
        const cells = this.row.querySelectorAll("td");
        cells.forEach(cell => {
            if(cell.type === "field"){
                cell.textContent = this.item[cell.name];
            }
        });
    }

    async editItem(){
        const newItem = this.generateItemFromInput();
        if(!this.isInputChanged(newItem)){
            document.getElementById("delete-edit-item-text").innerText = "No changes detected.";
            return;
        }
        const url = this.serverUrl + "/item/" + newItem.id;
        const jsonData = JSON.stringify(newItem);
        console.log("Updating item with ID: " + newItem.id + " with data: " + jsonData);
        this.addSpinner("delete-edit-item-text");
        let response;
        try{
            response = await fetch(url, {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: jsonData
            })
        } catch(error) {
            document.getElementById("delete-edit-item-text").innerText = "Error connecting to server.";
            return;
        }
        let responseData = null; //204 has no body, may cause error when calling response.json()
        if(response.status !== 204){
            responseData = await response.json();
        } else {
            this.item = Object.assign(this.item, newItem);
            this.switchToDisplayMode();
        }
        this.printEditItemResponseMessage(response, responseData);
    }

    generateItemFromInput(){
        const item = {};
        item.id = this.item.id;
        const cells = this.row.querySelectorAll("td");
        for(const cell of cells){
            if(cell.type === "field"){
                item[cell.name] = cell.querySelector("input").value.trim();
                if(cell.name === "amount"){
                    item[cell.name] = Number(item[cell.name]);
                }
            }
        }
        return item;
    }

    isInputChanged(newItem){
        for(const fieldName of this.fieldNameOrder){
            if(this.item[fieldName] !== newItem[fieldName]){
                return true;
            }
        }
        return false;
    }

    printEditItemResponseMessage(response, responseData){
        const textObj = document.getElementById("delete-edit-item-text");
        textObj.innerText = "";
        switch(response.status){
            case 204:
                textObj.innerText = "Item updated successfully!";
                break;
            case 400:
            case 404:
            case 500:
                textObj.innerText = responseData.errorMessage;
                break;
            default:
                textObj.innerText = "Unexpected error occurred.";
        }
    }

    createSpinner() {
        //loading icon that spins
        const spinner = document.createElement('div');
        spinner.classList.add('spinner');
        return spinner;
    }

    addSpinner(elementId){
        const element = document.getElementById(elementId);
        element.innerText = "";
        element.appendChild(this.createSpinner());
    }
}