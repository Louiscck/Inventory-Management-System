import RowObject from './row-object.js';

document.getElementById("add-item-button").addEventListener("click", addItem);
document.getElementById("get-item-button").addEventListener("click", getItem);
document.getElementById("amount-input").addEventListener("input", function(){
    this.value = Math.max(0,this.value);
});

async function addItem(){
    const form = document.getElementById("add-item-form");
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const jsonData = JSON.stringify(data);
    console.log("Creating resource: "+jsonData);

    const url = "http://localhost:8080/item";
    const response = await fetch(url, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: jsonData
    })
    if(response.status == 204){
        return; //has no body, may cause error when calling response.json()
    }
    const responseData = await response.json();
    printAddItemResponseMessage(response, responseData);
}

function printAddItemResponseMessage(response, responseData){
    const textObj = document.getElementById("add-item-button-text");
    textObj.innerText = "";
    switch(response.status){
        case 201:
            textObj.innerText = "Item added successfully!";
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

async function getItem(){
    const form = document.getElementById("get-item-form");
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const queryString = new URLSearchParams(data).toString();
    console.log("Search keyword: " + queryString);

    const url = "http://localhost:8080/item?" + queryString;
    const response = await fetch(url);
    if(response.status == 204){
        return; //has no body, may cause error when calling response.json()
    }
    const responseData = await response.json();
    if(response.status == 200){
        displayItemTable(responseData);
    }
    printGetItemResponseMessage(response, responseData);
}

function displayItemTable(responseData){
    const table = document.querySelector("#display-item-table tbody");
    const fieldNameOrder = ["name", "category", "specification", "unit", "amount"];
    //make sure fieldNameOrder is consistent with display order in table header
    table.innerHTML = "";
    responseData.forEach(item => addRow(table, item, fieldNameOrder));
}

function addRow(table, item, fieldOrder) {
    const rowObject = new RowObject(item, fieldOrder);
    rowObject.editButton.addEventListener("click", editItem);
    rowObject.deleteButton.addEventListener("click", deleteItem);
    table.appendChild(rowObject.row);
}

function printGetItemResponseMessage(response, responseData){
    const textObj = document.getElementById("get-item-text");
    textObj.innerText = "";
    switch(response.status){
        case 200:
            textObj.innerText = responseData.length + " search results found.";
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

async function deleteItem(event){
    const confirmed = confirm("Are you sure you want to delete this item?");
    if(!confirmed){
        return;
    }
    const button = event.currentTarget;
    const rowObject = button.rowObject;
    const item = rowObject.item;

    const url = "http://localhost:8080/item/" + item.id;
    console.log("Deleting item with ID: " + item.id);
    const response = await fetch(url, {
        method: "DELETE",
    })
    let responseData = null; //204 has no body, may cause error when calling response.json()
    if(response.status != 204){
        responseData = await response.json();
    }
    printDeleteItemResponseMessage(response, responseData);
    if(response.status == 204){
        const table = document.querySelector("#display-item-table tbody");
        table.removeChild(rowObject.row);
    }
}

function printDeleteItemResponseMessage(response, responseData){
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

async function editItem(event){
    const button = event.currentTarget;
    const rowObject = button.rowObject;
    const row = rowObject.row;
    let item = rowObject.item;
    const cells = row.querySelectorAll("td");
    if(button.mode === "edit"){
        button.mode = "submit";
        button.innerHTML = '<i class="bi bi-check2-all"></i>';
        cells.forEach(cell => addInputToCell(cell, item));
    } else if (button.mode === "submit"){
        button.mode = "edit";
        button.innerHTML = '<i class="bi bi-pencil-fill"></i>';
        if(!isInputChanged(cells, item)){
            document.getElementById("delete-edit-item-text").innerText = "No changes detected.";
        } else {
            const newItem = generateItemFromInput(item.id, cells);
            const url = "http://localhost:8080/item/" + newItem.id;
            const jsonData = JSON.stringify(newItem);
            console.log("Updating item with ID: " + newItem.id + " with data: " + jsonData);
            const response = await fetch(url, {
                method: "PUT",
                headers: {"Content-Type": "application/json"},
                body: jsonData
            })
            let responseData = null; //204 has no body, may cause error when calling response.json()
            if(response.status !== 204){
                responseData = await response.json();
            } else {
                item = Object.assign(item, newItem); 
                //update local item only if update successful so that restoreCellDisplay works correctly
            }
            printEditItemResponseMessage(response, responseData);
        }
        cells.forEach(cell => restoreCellDisplay(cell, item));
    }
}

function addInputToCell(cell, item){
    if(cell.type === "field"){
        cell.textContent = "";
        const value = item[cell.name];
        const input = document.createElement("input");
        input.type = "text";
        input.value = value;
        input.style.width = "100%";
        if(cell.name === "amount"){
            input.type = "number";
            input.min = "0";
            input.addEventListener("input",function(){
                this.value = Math.max(0,this.value);
            });
        }
        cell.appendChild(input);
    }
}

function isInputChanged(cells, item){
    let changed = false;
    for(const cell of cells){
        if(cell.type === "field"){
            const input = cell.querySelector("input");
            let value = input.value.trim();
            if(cell.name === "amount"){
                value = Number(value);
            }
            if(value !== item[cell.name]){
                changed = true;
                break;
            }
        }
    }
    return changed;
}

function restoreCellDisplay(cell, item){
    if(cell.type === "field"){
        cell.textContent = item[cell.name];
    }
}

function generateItemFromInput(id, cells){
    const item = {};
    item.id = id;
    for(const cell of cells){
        if(cell.type === "field"){
            item[cell.name] = cell.querySelector("input").value.trim();
            if(cell.name === "amount"){
                item[cell.name] = Number(item[cell.name]);
            }
        }
    }
    console.log("Generated item = " + JSON.stringify(item));
    return item;
}

function printEditItemResponseMessage(response, responseData){
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
