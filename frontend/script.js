document.getElementById("add-item-button").addEventListener("click", addItem);
document.getElementById("get-item-button").addEventListener("click", getItem);
document.getElementById("amount-input").addEventListener("input", function(){
    this.value = Math.max(0,this.value);
});

async function addItem(event){
    const form = document.getElementById("add-item-form");
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const jsonData = JSON.stringify(data);
    console.log(jsonData);

    const url = "http://localhost:8080/item";
    const response = await fetch(url, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: jsonData
    })
    printAddItemResponseMessage(response);
}

async function printAddItemResponseMessage(response){
    let responseData = null;
    if(response.headers.get("Content-Length") > 0){
        responseData = await response.json();
    }
    //response of status 204 has no body and response.json() will cause error
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
    const form = document.getElementById("get-item-form");
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const queryString = new URLSearchParams(data).toString();
    console.log(queryString);

    const url = "http://localhost:8080/item?" + queryString;
    const response = await fetch(url);
    let responseData = null;
    if(response.headers.get("Content-Length") > 0){
        responseData = await response.json();
    }
    //response of status 204 has no body and response.json() will cause error
    if(response.status == 200){
        displayItemTable(responseData);
    }
    printGetItemResponseMessage(response, responseData);
}

async function displayItemTable(responseData){
    console.log(responseData);
    const table = document.querySelector("#display-item-table tbody");
    const fieldOrder = ["name", "category", "specification", "unit", "amount"];
    table.innerHTML = "";
    responseData.forEach(item => addRow(table, item, fieldOrder));
}

function addRow(table, item, fieldOrder) {
    //make sure fieldOrder is consistent with display order in table
    const row = document.createElement("tr");
    fieldOrder.forEach(field => row.appendChild(addTableData(item[field])));
    table.appendChild(row);
}

function addTableData(item){
    const cell = document.createElement("td");
    cell.textContent = item;
    return cell;
}

async function printGetItemResponseMessage(response, responseData){
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
