if (window.console) {
    console.log("Welcome to Issue Resolution app's JavaScript!");
}
var tableIssues;

function loadIssues() {
    tableIssues = $('#issuesTable').DataTable({
        "processing": true,
        "serverSide": true,
        type: 'POST',
        "ajax": {
            "url": "/list",
            "type": "POST"
        },
        //"autoWidth": false,
        "columns": [
            {"data": "select"},
            {"data": "DT_RowId"},
            {"data": "status"},
            {"data": "dateLogged"},
            {"data": "participantId"},
            {"data": "dataSource"},
            {"data": "priority"},
            {"data": "dataItem"},
            {"data": "shortDesc"},
            {"data": "gmc"},
            {"data": "lsid"},
            {"data": "area"},
            {"data": "description"},
            {"data": "familyId"},
            {"data": "queryDate"},
            {"data": "weeksOpen"},
            {"data": "resolutionDate"},
            {"data": "escalation"}
        ],
        columnDefs: [ {
            orderable: false,
            className: 'select-checkbox',
            targets:   0
        }, {
            className: 'qchain',
            targets:   9
        } ],
        select: {
            style:    'os',
            selector: 'td:first-child'
        },
        order: [[ 3, 'desc' ]],
        buttons: [
            'copy', 'excel', 'pdf'
        ],
        "createdRow": function (row, data, index) {
            $('td', row).eq(9).html(data.gmc + "<span class='pull-right glyphicon glyphicon-th-list'></span>");
        }
    });
}

function displayQChain(el) {
    var issueId = tableIssues.row( el ).data().DT_RowId;
    var participantId = tableIssues.row( el ).data().patientId;

    var formData = new FormData();
    formData.append('selectedIssue', issueId);

    $.ajax({
        url: '/querychain',
        data: formData,
        type: 'POST',
        contentType: false,
        processData: false,
        success: function (data) {
            var qChains = "<tbody id='qChainTableBody'>";
            var qchainClass = "qchain-dq";

            for (var i = 0; i < data.length; i++) {

                if(data[i].partyid == 0)  qchainClass = "qchain-dq";
                else qchainClass = "qchain-gmc";

                qChains += "<tr class='" + qchainClass +"'><td><strong>" + data[i].status + "</strong></td><td class='text-left'>" + data[i].date + "</td><td class='text-right'>" + data[i].user + "</td></tr>";
                qChains += "<tr class='" + qchainClass +"'><td colspan='8'>" + data[i].comment + "</td></tr>";
            }
            qChains += "</tbody>";

            $("#qChainTableBody").replaceWith(qChains);
            $("#qChainIssueId").text(issueId + " / " + participantId);
            $("#qChainModal").modal({backdrop: 'static'});
            $("#qChainModal").modal('show');
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred, please see server logs:" + textStatus + ': ' + errorThrown);
        }
    });
}

function loadGMCs() {
    $.ajax({
        url: "/listGmcs",
        success: function (data) {
            if (data.length > 0) {

                var output = [];
                output.push('<option value="'+ 'all' +'">'+ 'all' +'</option>');

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

function loadOrigins() {
    $.ajax({
        url: "/listOrigins",
        success: function (data) {
            if (data.length > 0) {

                var output = [];
                output.push('<option value="'+ 'all' +'">'+ 'all' +'</option>');

                for (var i = 0; i < data.length; i++) {
                    output.push('<option value="'+ data[i].origin +'">'+ data[i].origin +'</option>');
                }
                $('#originSelect').html(output.join(''));

            } else {
                alert("no list of Data Sources to load - there may have been an error");
            }
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred loading Data Sources list, please see server logs:" + textStatus + ': ' + errorThrown);
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
            cssClass: 'failure-default',
            action: function(dialogItself){
                dialogItself.close();
                statusChangeConfirm("Open");
            }
        }, {
            label: 'Responded',
            cssClass: 'responded-default',
            action: function(dialogItself){
                dialogItself.close();
                statusChangeConfirm("Responded");
            }
        }, {
            label: 'Resolved',
            cssClass: 'ok-default',
            action: function(dialogItself){
                dialogItself.close();
                statusChangeConfirm("Resolved");
            }
        }, {
            label: 'Archived',
            cssClass: 'archived-default',
            action: function(dialogItself){
                dialogItself.close();
                statusChangeConfirm("Archived");
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

function buildFilter() {
    var gmc = $('#gmcSelect').val() == 'all' ? '' : '&gmc=' + $('#gmcSelect').val();
    var status = $('#statusSelect').val() == 'all' ? '' : '&status=' + $('#statusSelect').val();
    var origin = $('#originSelect').val() == 'all' ? '' : '&origin=' + $('#originSelect').val();
    return gmc + status + origin;
}

function filterTable(tableIssues, url, e) {
    tableIssues.ajax.url(url).load();
    e.preventDefault();
}

function resetInputs() {
    $('#gmcSelect').val('all');
    $('#statusSelect').val('all');
    $('#originSelect').val('all');
}

function getParameterByName(name, url) {
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

// TODO change function to use buildfilter if possible...otherwise eg. no. of days is hardcoded in 2 places
function exportCsv() {
    var currentFilter = tableIssues.ajax.url();

    var existingGmc = null;
    var status = null;
    var origin = null;
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
            if (params[i].startsWith("status=")) {
                status = params[i].substring(params[i].indexOf("=") + 1);
            }
            if (params[i].startsWith("origin=")) {
                origin = params[i].substring(params[i].indexOf("=") + 1);
            }
            if (params[i].startsWith("filter=new")) {
                isNew = true;
            }
        }
    } else {
        allIssues = true;
    }


    if (allIssues) {
        cleanHiddenInputs();
    }

    if (existingGmc || status) {
        cleanHiddenInputs();
        if (existingGmc) {
            $('#exportIssuesForm').append('<input type="hidden" id="gmc" name="gmc" value="" />');
            $(':hidden#gmc').val(existingGmc);
        }
        if (status) {
            $('#exportIssuesForm').append('<input type="hidden" id="status" name="status" value="" />');
            $(':hidden#status').val(status);
        }
        if (origin) {
            $('#exportIssuesForm').append('<input type="hidden" id="origin" name="origin" value="" />');
            $(':hidden#origin').val(origin);
        }
    }
    if (isNew) {
        cleanHiddenInputs();
        $('#exportIssuesForm').append('<input type="hidden" id="filter" name="filter" value="new" />');
        $('#exportIssuesForm').append('<input type="hidden" id="days" name="days" value="30" />');
    }

    $(':hidden#length').val("60000");       //TODO not sure what limit if any should be applied

    function cleanHiddenInputs() {
        $(':hidden#filter').remove();
        $(':hidden#days').remove();
        $(':hidden#gmc').remove();
        $(':hidden#status').remove();
        $(':hidden#origin').remove();
    }
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

function reportDisplay(name) {
    var formData = new FormData();
    $.ajax({
        url: '/reports',
        data: formData,
        contentType: false,
        processData: false,
        success: function (data) {
            var errorRows = "<tbody id='reportTableBody'>";

            for (var i = 0; i < data.length; i++) {
                errorRows += "<tr><td>" + data[i].outstanding + "</td><td>" + data[i].resolved + "</td>"
                 + "<td>" + data[i].qtime + "</td><td>" + data[i].qitem + "</td></tr>";
            }
            errorRows += "</tbody>";

            $("#reportTableBody").replaceWith(errorRows);
            $("#reportsModal").modal({backdrop: 'static'});
            $("#reportsModal").modal('show');
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
    loadOrigins();
    resetInputs();

    $("#allIssues").click(function (e) {
        resetInputs();
        var url = "/list?" + buildFilter();
        filterTable(tableIssues, "/list", e);
    });

    $("#newIssues").click(function (e) {
        resetInputs();
        var url = "/list?filter=new&days=30";   //should be last 30days by default
        filterTable(tableIssues, url, e);
    });

    $('#gmcSelect').change(function(e) {
        var url = "/list?" + buildFilter();
        filterTable(tableIssues, url, e);
    });

    $('#statusSelect').change(function(e) {
        var url = "/list?" + buildFilter();
        filterTable(tableIssues, url, e);
    });

    $('#originSelect').change(function(e) {
        var url = "/list?" + buildFilter();
        filterTable(tableIssues, url, e);
    });

    $("#sendButton").click(function (e) {
        sendSelected(e);
    });

    $("#statusButton").click(function (e) {
        statusChangeDialog(e);
    });

    $("#reportButton").click(function (e) {
        reportDisplay(e);
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

    $(document ).on( "click", "TD.qchain", function() {
        displayQChain(this);
    });

});


