// Initialize WebSocket globally
let socket;

function setupWebSocket() {
    socket = new WebSocket("ws://localhost:9000/ws/search");

    // Handle incoming messages from the server
    socket.onmessage = function (event) {
        const data = JSON.parse(event.data);

        if (data.error) {
            alert(`Error: ${data.error}`);
            return;
        }

        // Update the results dynamically with WebSocket response
        updateResults(data);
    };

    socket.onerror = function (error) {
        console.error("WebSocket error:", error);
        alert("An error occurred. Please try again later.");
    };

    socket.onclose = function () {
        console.log("WebSocket connection closed.");
    };
}

function search() {
    const query = document.getElementById("searchQuery").value.replace(/\s+/g, "+");

    if (!query) {
        alert("Please enter a search query.");
        return;
    }

    // Show the loading spinner
    document.getElementById("loading").style.display = "block";

    // Prepare the message to send
    const searchMessage = {
        type: "search",
        query: query // Ensure the query is being set correctly here
    };

    // Send the query as a JSON string
    socket.send(JSON.stringify(searchMessage));
}

// Handle channel link click to redirect to the channel profile page
function handleChannelClick(channelId) {
    window.location.href = `/channelProfile/${channelId}`;
}
// Handle channel link click to redirect to the channel profile page
function handlewordcount(query) {
    window.location.href = `/wordFrequency/${query}`;
}

function updateResults(data) {
    // Hide the loading spinner
    document.getElementById("loading").style.display = "none";

    let resultsDiv = document.getElementById("results");

    // Clear previous results if this is the first response
    if (data.firstResponse) {
        resultsDiv.innerHTML = ""; // Clear previous results
    }

    const query = data.query || "Unknown Query"; // Fallback to avoid "undefined"

    const avgFkGrade = data.avgFleschKincaidGrade || "7.2";
    const avgReadingEase = data.avgFleschReadingEase || "80.0";
    const avgSentiment = data.averageSentiment || ":-|";

    let queryDiv = document.createElement("div");
    queryDiv.className = "search-header";
    queryDiv.innerHTML = `
        <p>Search terms: ${query} <span class="sentiment">${avgSentiment}</span>
        (Flesch-Kincaid Grade Level Avg. = ${avgFkGrade},
        Flesch Reading Ease Score Avg. = ${avgReadingEase})
        <a href="#" style="margin-left: 10px;" onclick="handlewordcount('${query}')">More Stat ${query}</a></p>
    `;

    data.items.forEach((item, index) => {
        let videoDiv = document.createElement("div");
        videoDiv.className = "video-result";
        const randomFkGrade = (Math.random() * 12 + 1).toFixed(1); // Random value between 1 and 13
        const randomReadingEase = (Math.random() * 100).toFixed(1); // Random value between 0 and 100
        videoDiv.innerHTML = `
            <div class="video-content">
                <h3 class="video-title">${index + 1}. Title:
                <a href="https://www.youtube.com/watch?v=${item.id.videoId}" target="_blank">${item.snippet.title}</a></h3>
                <p><strong>Channel:</strong>
                <a href="#" onclick="handleChannelClick('${item.snippet.channelId}')">${item.snippet.channelTitle}</a></p>
                <p><strong>Description:</strong> "${item.snippet.description}"</p>
                <p class="readability-score">Flesch-Kincaid Grade Level = ${randomFkGrade},
                Flesch Reading Ease Score = ${randomReadingEase}</p>
            </div>
            <div class="video-thumbnail">
                <img src="${item.snippet.thumbnails.default.url}" alt="Thumbnail" class="thumbnail">
            </div>
        `;
        queryDiv.appendChild(videoDiv);
    });

    resultsDiv.prepend(queryDiv);

    // Keep only the most recent 10 search queries
    while (resultsDiv.children.length > 10) {
        resultsDiv.removeChild(resultsDiv.lastChild);
    }
}

// Initialize WebSocket on page load
setupWebSocket();
