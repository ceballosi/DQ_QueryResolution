if (window.console) {
    console.log("Welcome to Issue Resolution app's JavaScript!");
}

function loadIssues() {
    $('#issuesTable').DataTable({
        //"deferRender": true
        "processing": true,
        "serverSide": true,
        "ajax": "/list",
        "columns": [
            {"data": "status"},
            {"data": "DT_RowId"},
            {"data": "loggedBy"},
            {"data": "dateLogged"},
            {"data": "issueOrigin"},
            {"data": "GMC"},
            {"data": "description"},
            {"data": "familyId"}
        ]
    });
}


function filterTable(tableIssues, url, e) {
    tableIssues.ajax.url(url).load();
    e.preventDefault();
}

$(document).ready(function () {
    loadIssues();
    var tableIssues = $('#issuesTable').DataTable();


    $("#allIssues").click(function (e) {
        filterTable(tableIssues, "/list", e);
    });

    $("#newIssues").click(function (e) {
        var url = "/list?filter=new&days=45";   //should be last 30days by default
        filterTable(tableIssues, url, e);
    });

    $(".gmc").click(function (e) {
        var url = "/list?gmc=" + e.target.innerText;
        filterTable(tableIssues, url, e);
    });

});


