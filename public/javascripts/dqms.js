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

function getParameterByName(name, url) {
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

$(document).ready(function () {
    loadIssues();
    tableIssues = $('#issuesTable').DataTable();


    $("#allIssues").click(function (e) {
        filterTable(tableIssues, "/list", e);
    });

    $("#newIssues").click(function (e) {
        var url = "/list?filter=new&days=30";   //should be last 30days by default
        filterTable(tableIssues, url, e);
    });

    $(".gmc").click(function (e) {
        var url = "/list?gmc=" + e.target.innerText;
        filterTable(tableIssues, url, e);
    });

    $("#submitButton").click(function (e) {
        submitSelected(e);
    });


    $("#export").click(function (e) {
        var currentFilter = tableIssues.ajax.url();

        var existingGmc = null;
        var isNew = false;
        var params = currentFilter.split("?")[1].split("&");
        for(i=0;i<params.length;i++){
            if(params[i].startsWith("gmc=")) {
                existingGmc = params[i].substring( params[i].indexOf("=") + 1);
            }
            if(params[i].startsWith("filter=new")) {
                isNew = true;
            }
        }

        if(existingGmc) {
            $(':hidden#filter').remove();
            $(':hidden#days').remove();
            $('#exportIssuesForm').append('<input type="hidden" id="gmc" name="gmc" value="" />');
            $(':hidden#gmc').val(existingGmc);
        }
        if(isNew) {
            $(':hidden#gmc').remove();
            $('#exportIssuesForm').append('<input type="hidden" id="filter" name="filter" value="new" />');
            $('#exportIssuesForm').append('<input type="hidden" id="days" name="days" value="45" />');
        }

        $(':hidden#length').val("60000");       //TODO not sure what limit if any should be applied

    });

});


