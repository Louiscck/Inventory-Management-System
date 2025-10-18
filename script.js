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
    if(response.status === 201){
        document.getElementById("add-item-button-text").innerText = "Item added successfully!";
    }
}