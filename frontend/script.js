document.getElementById("add-item-button").addEventListener("click", addItem);

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
    let responseData = null;
    if(response.headers.get("Content-Length") > 0){
        responseData = await response.json();
    }
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