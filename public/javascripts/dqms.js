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


$(document).ready(function () {
    loadIssues();
    var tableIssues = $('#issuesTable').DataTable();
});


