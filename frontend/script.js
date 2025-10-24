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
    const fieldOrder = ["name", "category", "specification", "unit", "amount"];
    //make sure fieldOrder is consistent with display order in table header
    table.innerHTML = "";
    responseData.forEach(item => addRow(table, item, fieldOrder));
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

function editItem(event){

}
