if (window.console) {
    console.log("Welcome to Issue Resolution app's JavaScript!");
}
var tableIssues;

function loadIssues() {
    tableIssues = $('#issuesTable').DataTable({
        "processing": true,
        "serverSide": true,
        "ajax": "/list",
        "columns": [
            {"data": "select"},
            {"data": "status"},
            {"data": "DT_RowId"},
            {"data": "loggedBy"},
            {"data": "dateLogged"},
            {"data": "issueOrigin"},
            {"data": "GMC"},
            {"data": "description"},
            {"data": "patientId"}
        ],
        columnDefs: [ {
            orderable: false,
            className: 'select-checkbox',
            targets:   0
        } ],
        select: {
            style:    'os',
            selector: 'td:first-child'
        },
        order: [[ 4, 'desc' ]],
        buttons: [
            'copy', 'excel', 'pdf'
        ]
    });
}


function submitSelected(e) {
    var selectedIds = new Array();

    if (tableIssues.rows({selected: true}).count() > 0) {

        tableIssues.rows({selected: true}).data().each(function (rowData) {
            selectedIds.push(rowData.DT_RowId);
        });

        var currentFilter = tableIssues.ajax.url();
        url = "/send?selectedIssues=" + selectedIds.join();
        tableIssues.ajax.url(url).load();
        tableIssues.ajax.url(currentFilter).load();
    }
    e.preventDefault();
}

function filterTable(tableIssues, url, e) {
    tableIssues.ajax.url(url).load();
    e.preventDefault();
}

$(document).ready(function () {
    loadIssues();
    tableIssues = $('#issuesTable').DataTable();


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

    $("#submitButton").click(function (e) {
        submitSelected(e);
    });

    //tableIssues.buttons().container().appendTo( $('.col-sm-6:eq(0)', tableIssues.table().container() ) );
});


