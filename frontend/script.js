import RowObject from './row-object.js';

document.getElementById("add-item-button").addEventListener("click", addItem);
document.getElementById("get-item-button").addEventListener("click", getItem);
document.getElementById("amount-input").addEventListener("input", function(){
    this.value = Math.max(0,this.value);
});

const serverUrl = "https://inventory-management-system-znti.onrender.com";

async function addItem(event){
    event.preventDefault();
    const form = document.getElementById("add-item-form");
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const jsonData = JSON.stringify(data);
    console.log("Creating resource: "+jsonData);

    const url = serverUrl + "/item";
    const response = await fetch(url, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: jsonData
    })
    let responseData = null;
    if(response.status !== 201){
        responseData = await response.json(); //201 has no body, may cause error when calling response.json()
    }
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

async function getItem(event){
    event.preventDefault();
    const form = document.getElementById("get-item-form");
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const queryString = new URLSearchParams(data).toString();
    console.log("Search keyword: " + queryString);

    const url = serverUrl + "/item?" + queryString;
    const response = await fetch(url);
    if(response.status === 204){
        return; //has no body, may cause error when calling response.json()
    }
    const responseData = await response.json();
    if(response.status === 200){
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
    const rowObject = new RowObject(item, fieldOrder, serverUrl);
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
