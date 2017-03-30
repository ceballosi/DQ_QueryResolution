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


function loadGMCs() {
    $.ajax({
        url: "/listGmcs",
        success: function (data) {
            if (data.length > 0) {

                var output = [];
                for (var i = 0; i < data.length; i++) {
                    output.push('<option value="'+ data[i].gmc +'">'+ data[i].gmc +'</option>');
                }
                $('#gmcSelect').html(output.join(''));

            } else {
                alert("no GMCs to load - there may have been an error");
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred loading GMCs, please see server logs:" + textStatus + ': ' + errorThrown);
        }
    });
}


function sendSelected(e) {
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

function statusChangeDialog(e) {
    var statusDialog = new BootstrapDialog({
        title: 'Change Status',
        message: 'Select new status for selected issues',
        buttons: [{
            label: 'Draft',
            cssClass: 'btn-confirm',
            action: function(dialogItself){
                dialogItself.close();
                statusChangeConfirm("Draft");
            }
        }, {
            label: 'Open',
            cssClass: 'ok-default',
            action: function(dialogItself){
                dialogItself.close();
                statusChangeConfirm("Open");
            }
        }, {
            label: 'Closed',
            cssClass: 'failure-default',
            action: function(dialogItself){
                dialogItself.close();
                statusChangeConfirm("Closed");
                statusDialog.close();
            }
        }]
    });
    statusDialog.realize();
    statusDialog.getModalFooter().css("text-align", "center");
    statusDialog.open();
}

function statusChangeConfirm(selectedStatus) {
    BootstrapDialog.confirm({
        title: 'Status Change',
        message: 'Are you sure you want to change selected issues to: ' +selectedStatus+ '?',
        type: BootstrapDialog.TYPE_WARNING,
        btnOKLabel: 'Change Status',
        btnOKClass: 'btn-warning',
        callback: function (result) {
            if (result) {
                statusChange(selectedStatus);
            }
        }
    });
}
function statusChange(selectedStatus) {
    var selectedIds = new Array();

    if (tableIssues.rows({selected: true}).count() > 0) {

        tableIssues.rows({selected: true}).data().each(function (rowData) {
            selectedIds.push(rowData.DT_RowId);
        });

        var currentFilter = tableIssues.ajax.url();
        var formData = new FormData();
        formData.append('change', selectedStatus);
        formData.append('selectedIssues', selectedIds);

        $.ajax({
            url: '/status',
            data: formData,
            type: 'POST',
            contentType: false,
            processData: false,
            success: function (data) {
                if (data == "OK" || data.length == 0) {
                    BootstrapDialog.show({
                        title: 'Status Change Successful',
                        size: BootstrapDialog.SIZE_SMALL,
                        type: BootstrapDialog.TYPE_SUCCESS
                    });
                } else {
                    var errorRows = "<tbody id='failuresTableBody'>";

                    for (var i = 0; i < data.length; i++) {
                        errorRows += "<tr><td>" + data[i].rownum + "</td><td>" + data[i].error + "<td></tr>";
                    }
                    errorRows += "</tbody>";

                    $("#statusErrTableBody").replaceWith(errorRows);
                    $("#statusErrors").modal({ backdrop: 'static'});
                    $("#statusErrors").modal('show');
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                alert("An unexpected error occurred, please see server logs:" + textStatus + ': ' + errorThrown);
            }
        });
        setTimeout(function () {
            tableIssues.ajax.url(currentFilter).load();
        }, 2000);
    }
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

function exportCsv() {
    var currentFilter = tableIssues.ajax.url();

    var existingGmc = null;
    var isNew = false;
    var allIssues = false;
    var split = currentFilter.split("?");
    if (split.length > 1) {
        allIssues = false;

        var params = split[1].split("&");
        for (i = 0; i < params.length; i++) {
            if (params[i].startsWith("gmc=")) {
                existingGmc = params[i].substring(params[i].indexOf("=") + 1);
            }
            if (params[i].startsWith("filter=new")) {
                isNew = true;
            }
        }
    } else {
        allIssues = true;
    }

    if (allIssues) {
        $(':hidden#filter').remove();
        $(':hidden#days').remove();
        $(':hidden#gmc').remove();
    }
    if (existingGmc) {
        $(':hidden#filter').remove();
        $(':hidden#days').remove();
        $('#exportIssuesForm').append('<input type="hidden" id="gmc" name="gmc" value="" />');
        $(':hidden#gmc').val(existingGmc);
    }
    if (isNew) {
        $(':hidden#gmc').remove();
        $('#exportIssuesForm').append('<input type="hidden" id="filter" name="filter" value="new" />');
        $('#exportIssuesForm').append('<input type="hidden" id="days" name="days" value="45" />');
    }

    $(':hidden#length').val("60000");       //TODO not sure what limit if any should be applied
}


function importCsv(name) {
    var file = $('#importFile').get(0).files[0];
    var formData = new FormData();
    formData.append('file', file);
    $.ajax({
        url: '/upload',
        data: formData,
        type: 'POST',
        contentType: false,
        processData: false,
        beforeSend: function (data) {
            $("#importConfirm").modal('hide');
        },
        success: function (data) {
            if (data == "OK" || data.length == 0) {
                BootstrapDialog.show({
                    title: 'Import Successful',
                    size: BootstrapDialog.SIZE_SMALL,
                    type: BootstrapDialog.TYPE_SUCCESS
                });
            } else {
                var errorRows = "<tbody id='failuresTableBody'>";

                for (var i = 0; i < data.length; i++) {
                    errorRows += "<tr><td>" + data[i].rownum + "</td><td>" + data[i].error + "<td></tr>";
                }
                errorRows += "</tbody>";

                $("#failuresTableBody").replaceWith(errorRows);
                $("#importErrors").modal({ backdrop: 'static'});
                $("#importErrors").modal('show');
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred, please see server logs:" + textStatus + ': ' + errorThrown);
        }
    });
    return false;
}


//import event setup
$(function() {
    // attach `fileselect` event to all file inputs
    $(document).on('change', ':file', function() {
        var input = $(this),
            numFiles = input.get(0).files ? input.get(0).files.length : 1,
            label = input.val().replace(/\\/g, '/').replace(/.*\//, '');
        input.trigger('fileselect', [numFiles, label]);
    });

    // watch for custom `fileselect` event
    $(document).ready( function() {
        $(':file').on('fileselect', function(event, numFiles, label) {

            var input = $(this).parents('.input-group').find(':text'),
                selectedFile = numFiles > 1 ? numFiles + ' files selected' : label;

            if( input.length ) {
                input.val(selectedFile);
            }
            $("#importFilename").text("File '" + selectedFile + "' will be imported into the database.");
            $("#importConfirm").modal('show');
        });
    });

});



$(document).ready(function () {
    loadIssues();
    tableIssues = $('#issuesTable').DataTable();
    loadGMCs();

    $("#allIssues").click(function (e) {
        filterTable(tableIssues, "/list", e);
    });

    $("#newIssues").click(function (e) {
        var url = "/list?filter=new&days=30";   //should be last 30days by default
        filterTable(tableIssues, url, e);
    });

    $('#gmcSelect').change(function(e) {
        var url = "/list?gmc=" + this.value;
        filterTable(tableIssues, url, e);
    });

    $("#sendButton").click(function (e) {
        sendSelected(e);
    });

    $("#statusButton").click(function (e) {
        statusChangeDialog(e);
    });


    $("#export").click(function (e) {
        exportCsv();
    });


    $("#proceedImport").click(function (e) {
        var selectedFile = $("#importFile").get(0).files[0].name;
        if(selectedFile.length) {
            importCsv(selectedFile);
        }
    });

});


