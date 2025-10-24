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
    if(response.status == 204){
        return; //has no body, may cause error when calling response.json()
    }
    let responseData = await response.json();
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
    const form = document.getElementById("get-item-form");
    const formData = new FormData(form);
    const data = Object.fromEntries(formData.entries());
    const queryString = new URLSearchParams(data).toString();
    console.log(queryString);

    const url = "http://localhost:8080/item?" + queryString;
    const response = await fetch(url);
    if(response.status == 204){
        return; //has no body, may cause error when calling response.json()
    }
    let responseData = await response.json();
    if(response.status == 200){
        displayItemTable(responseData);
    }
    printGetItemResponseMessage(response, responseData);
}

function displayItemTable(responseData){
    console.log(responseData);
    const table = document.querySelector("#display-item-table tbody");
    const fieldOrder = ["name", "category", "specification", "unit", "amount"];
    //make sure fieldOrder is consistent with display order in table header
    table.innerHTML = "";
    responseData.forEach(item => addRow(table, item, fieldOrder));
}

function addRow(table, item, fieldOrder) {
    const row = document.createElement("tr");
    fieldOrder.forEach(field => row.appendChild(addTableData(item[field])));
    table.appendChild(row);
}

function addTableData(item){
    const cell = document.createElement("td");
    cell.textContent = item;
    return cell;
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
