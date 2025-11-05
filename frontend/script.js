import RowObject from './row-object.js';

//TODO 

//6. Add section for my resume link and github link

document.getElementById("add-item-button").addEventListener("click", addItem);
document.getElementById("get-item-button").addEventListener("click", getItem);
document.getElementById("amount-input").addEventListener("input", function(){
    this.value = Math.max(0,this.value);
});

const serverUrl = "https://inventory-management-system-znti.onrender.com";
//const serverUrl = "http://localhost:8080";
wakeUpServer();

async function wakeUpServer(){ //server wakey wakey, time to work
    //Hosting on Render free plan causes the server to sleep after 15 minutes of inactivity
    //sends a dummy request to wake up the server when the webpage is loading, server takes up to 1 min to wake up
    const text = document.getElementById("server-wake-up-status-text");
    const spinnerDiv = document.getElementById("spinner-div");
    const spinner = createSpinner();
    spinnerDiv.appendChild(spinner);
    text.innerText = "Waking up server, please wait...";
    try{
        const response = await fetch(serverUrl + "/ping");
        text.innerText = "Server is live.";
    } catch(error) {
        text.innerText = "Error connecting to server.";
    }
    spinnerDiv.removeChild(spinner);
}

function createSpinner() {
    //loading icon that spins
    const spinner = document.createElement('div');
    spinner.classList.add('spinner');
    return spinner;
}

function addSpinner(elementId){
    const element = document.getElementById(elementId);
    element.innerText = "";
    element.appendChild(createSpinner());
}

async function addItem(event){
    event.preventDefault();
    const form = document.getElementById("add-item-form");
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const jsonData = JSON.stringify(data);
    console.log("Creating resource: "+jsonData);

    const url = serverUrl + "/item";
    addSpinner("add-item-text");
    let response;
    try{
        response = await fetch(url, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: jsonData
        })
    } catch(error) {
        document.getElementById("add-item-text").innerText = "Error connecting to server.";
        return;
    }
    let responseData = null;
    if(response.status !== 201){
        responseData = await response.json(); //201 has no body, may cause error when calling response.json()
    }
    printAddItemResponseMessage(response, responseData);
}

function printAddItemResponseMessage(response, responseData){
    const textObj = document.getElementById("add-item-text");
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
    addSpinner("get-item-text");
    let response;
    try{
        response = await fetch(url);
    } catch(error) {
        document.getElementById("get-item-text").innerText = "Error connecting to server.";
        return;
    }
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
