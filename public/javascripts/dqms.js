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
            "url": "/dqms/list",
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
            {"data": "openDate"},
            {"data": "weeksOpen"},
            {"data": "resolutionDate"},
            {"data": "escalation"},
            {"data": "notes"},
        ],
        columnDefs: [{
            orderable: false,
            className: 'select-checkbox',
            targets: 0
        }, {
            className: 'qchain',
            targets: 9
        }],
        select: {
            style: 'os',
            selector: 'td:first-child'
        },
        order: [[3, 'desc']],
        buttons: [
            'copy', 'excel', 'pdf'
        ],
        "createdRow": function (row, data, index) {
            //Qchain
            //$('td', row).eq(9).html(data.gmc + "<span class='pull-right glyphicon glyphicon-th-list'></span>");
        }
    });
}


function displayQChain(el) {
    var issueId = tableIssues.row(el).data().DT_RowId;
    var participantId = tableIssues.row(el).data().patientId;

    var formData = new FormData();
    formData.append('selectedIssue', issueId);

    $.ajax({
        url: '/dqms/querychain',
        data: formData,
        type: 'POST',
        contentType: false,
        processData: false,
        success: function (data) {
            var qChains = "<tbody id='qChainTableBody'>";
            var qchainClass = "qchain-dq";

            for (var i = 0; i < data.length; i++) {

                if (data[i].partyid == 0)  qchainClass = "qchain-dq";
                else qchainClass = "qchain-gmc";

                qChains += "<tr class='" + qchainClass + "'><td><strong>" + data[i].status + "</strong></td><td class='text-left'>" + data[i].date + "</td><td class='text-right'>" + data[i].user + "</td></tr>";
                qChains += "<tr class='" + qchainClass + "'><td colspan='8'>" + data[i].comment + "</td></tr>";
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
        url: "/dqms/listGmcs",
        success: function (data) {
            if (data.length > 0) {

                var output = [];
                output.push('<option value="' + 'all' + '">' + 'all' + '</option>');

                for (var i = 0; i < data.length; i++) {
                    output.push('<option value="' + data[i].gmc + '">' + data[i].gmc + '</option>');
                }
                $('#gmcSelect').html(output.join(''));

            } else {
                alert("no GMCs to load - there may have been an error");
            }
            applyFilterHighlights();
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred loading GMCs, please see server logs:" + textStatus + ': ' + errorThrown);
        }
    });
}

function loadOrigins() {
    $.ajax({
        url: "/dqms/listOrigins",
        success: function (data) {
            if (data.length > 0) {

                var output = [];
                output.push('<option value="' + 'all' + '">' + 'all' + '</option>');

                for (var i = 0; i < data.length; i++) {
                    output.push('<option value="' + data[i].origin + '">' + data[i].origin + '</option>');
                }
                $('#originSelect').html(output.join(''));

            } else {
                alert("no list of Data Sources to load - there may have been an error");
            }
            applyFilterHighlights();
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred loading Data Sources list, please see server logs:" + textStatus + ': ' + errorThrown);
        }
    });
}
function loadPriorities() {
    $.ajax({
        url: "/dqms/listPriorities",
        success: function (data) {
            if (data.length > 0) {

                var output = [];
                output.push('<option value="' + 'all' + '">' + 'all' + '</option>');

                for (var i = 0; i < data.length; i++) {
                    output.push('<option value="' + data[i].priority + '">' + data[i].priority + '</option>');
                }
                $('#prioritySelect').html(output.join(''));

            } else {
                alert("no priorities to load - there may have been an error");
            }
            applyFilterHighlights();
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred loading Priorities, please see server logs:" + textStatus + ': ' + errorThrown);
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
        url = "/dqms/send?selectedIssues=" + selectedIds.join();
        tableIssues.ajax.url(url).load();
        tableIssues.ajax.url(currentFilter).load();
    }
    e.preventDefault();
}

function statusChangeDialog(e) {
    var statusDialog = new BootstrapDialog({
        title: 'Change Status',
        message: 'Choose new status for selected issues',
        buttons: [{
            label: 'Draft',
            cssClass: 'btn-confirm',
            action: function (dialogItself) {
                dialogItself.close();
                statusChangeConfirm("Draft");
            }
        }, {
            label: 'Open',
            cssClass: 'failure-default',
            action: function (dialogItself) {
                dialogItself.close();
                statusChangeConfirm("Open");
            }
        }, {
            label: 'Responded',
            cssClass: 'responded-default',
            action: function (dialogItself) {
                dialogItself.close();
                statusChangeConfirm("Responded");
            }
        }, {
            label: 'Resolved',
            cssClass: 'ok-default',
            action: function (dialogItself) {
                dialogItself.close();
                statusChangeConfirm("Resolved");
            }
        }, {
            label: 'Archived',
            cssClass: 'archived-default',
            action: function (dialogItself) {
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
        message: 'Are you sure you want to change selected issues to: ' + selectedStatus + '?',
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
            url: '/dqms/changeStatus',
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
                } else if (data.startsWith("Change Status failed")) {
                    alert("An unexpected error occurred, please see server logs:" + data);
                } else {
                    var errorRows = "<tbody id='failuresTableBody'>";

                    for (var i = 0; i < data.length; i++) {
                        errorRows += "<tr><td>" + data[i].rownum + "</td><td>" + data[i].error + "<td></tr>";
                    }
                    errorRows += "</tbody>";

                    $("#statusErrTableBody").replaceWith(errorRows);
                    $("#statusErrors").modal({backdrop: 'static'});
                    $("#statusErrors").modal('show');
                }
                tableIssues.ajax.url(currentFilter).load();
            },
            error: function (jqXHR, textStatus, errorThrown) {
                alert("An unexpected error occurred, please see server logs:" + textStatus + ': ' + errorThrown);
            }
        });
    }
}

function applyFilterHighlights() {
    if ($('#gmcSelect').val() == 'all') {
        $('#gmcSelect').removeClass('dqmsSelected');
    } else {
        $('#gmcSelect').addClass('dqmsSelected');
    }
    if ($('#statusSelect').val() == 'all') {
        $('#statusSelect').removeClass('dqmsSelected');
    } else {
        $('#statusSelect').addClass('dqmsSelected');
    }
    if ($('#originSelect').val() == 'all') {
        $('#originSelect').removeClass('dqmsSelected');
    } else {
        $('#originSelect').addClass('dqmsSelected');
    }
    if ($('#prioritySelect').val() == 'all') {
        $('#prioritySelect').removeClass('dqmsSelected');
    } else {
        $('#prioritySelect').addClass('dqmsSelected');
    }
    if ($('#areaSelect').val() == 'all') {
        $('#areaSelect').removeClass('dqmsSelected');
    } else {
        $('#areaSelect').addClass('dqmsSelected');
    }
    if ($('#startDate').val() == '') {
        $('#startDate').removeClass('dqmsSelected');
    } else {
        $('#startDate').addClass('dqmsSelected');
    }
    if ($('#endDate').val() == '') {
        $('#endDate').removeClass('dqmsSelected');
    } else {
        $('#endDate').addClass('dqmsSelected');
    }
}

function buildFilter() {
    var gmc = $('#gmcSelect').val() == 'all' ? '' : '&gmc=' + $('#gmcSelect').val();
    var status = $('#statusSelect').val() == 'all' ? '' : '&status=' + $('#statusSelect').val();
    var origin = $('#originSelect').val() == 'all' ? '' : '&origin=' + $('#originSelect').val();
    var priority = $('#prioritySelect').val() == 'all' ? '' : '&priority=' + $('#prioritySelect').val();
    var area = $('#areaSelect').val() == 'all' ? '' : '&area=' + $('#areaSelect').val();
    var startDate = $('#startDate').val() == '-' ? '' : '&startDate=' + $('#startDate').val();
    var endDate = $('#endDate').val() == '-' ? '' : '&endDate=' + $('#endDate').val();

    applyFilterHighlights();

    return gmc + status + origin + priority + area + startDate + endDate;
}

function filterTable(tableIssues, url, e) {
    tableIssues.ajax.url(url).load();
    e.preventDefault();
}

function resetInputs() {
    $('#gmcSelect').val('all');
    $('#statusSelect').val('all');
    $('#originSelect').val('all');
    $('#prioritySelect').val('all');
    $('#areaSelect').val('all');
    $('#startDate').val('');
    $('#endDate').val('');
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
    var priority = null;
    var area = null;
    var startDate = null;
    var endDate = null;
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
            if (params[i].startsWith("priority=")) {
                priority = params[i].substring(params[i].indexOf("=") + 1);
            }
            if (params[i].startsWith("area=")) {
                area = params[i].substring(params[i].indexOf("=") + 1);
            }
            if (params[i].startsWith("startDate=")) {
                startDate = params[i].substring(params[i].indexOf("=") + 1);
            }
            if (params[i].startsWith("endDate=")) {
                endDate = params[i].substring(params[i].indexOf("=") + 1);
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

    if (existingGmc || status || origin || priority || area || startDate || endDate) {
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
        if (priority) {
            $('#exportIssuesForm').append('<input type="hidden" id="priority" name="priority" value="" />');
            $(':hidden#priority').val(priority);
        }
        if (area) {
            $('#exportIssuesForm').append('<input type="hidden" id="area" name="area" value="" />');
            $(':hidden#area').val(area);
        }
        if (startDate) {
            $('#exportIssuesForm').append('<input type="hidden" id="startDatehidden" name="startDate" value="" />');
            $(':hidden#startDatehidden').val(startDate);
        }
        if (endDate) {
            $('#exportIssuesForm').append('<input type="hidden" id="endDatehidden" name="endDate" value="" />');
            $(':hidden#endDatehidden').val(endDate);
        }
    }
    if (isNew) {
        cleanHiddenInputs();
        $('#exportIssuesForm').append('<input type="hidden" id="filter" name="filter" value="new" />');
        $('#exportIssuesForm').append('<input type="hidden" id="days" name="days" value="30" />');
    }

    //add sort to export from table
    var sortCol = tableIssues.order()[0][0];
    var sortDir = tableIssues.order()[0][1];
    $('#exportIssuesForm').append('<input type="hidden" id="sortColKey" name="order[0][column]" value="" />');
    $(':hidden#sortColKey').val(sortCol);
    $('#exportIssuesForm').append('<input type="hidden" id="sortDirKey" name="order[0][dir]" value="" />');
    $(':hidden#sortDirKey').val(sortDir);

    $(':hidden#length').val("60000");       //TODO not sure what limit if any should be applied

}

function cleanHiddenInputs() {
    $(':hidden#filter').remove();
    $(':hidden#days').remove();
    $(':hidden#gmc').remove();
    $(':hidden#status').remove();
    $(':hidden#origin').remove();
    $(':hidden#priority').remove();
    $(':hidden#area').remove();
    $(':hidden#startDatehidden').remove();  //id name startDate/endDate clash with main form
    $(':hidden#endDatehidden').remove();
    $(':hidden#sortColKey').remove();
    $(':hidden#sortDirKey').remove();
}

function resetExportButton() {
    $("#export").text("Export filtered");
}

function exportSelected(e) {
    $(':hidden#exportSelectedIds').val('');
    var selectedIds = new Array();

    if (tableIssues.rows({selected: true}).count() > 0) {

        tableIssues.rows({selected: true}).data().each(function (rowData) {
            selectedIds.push(rowData.DT_RowId);
        });
        $(':hidden#exportSelectedIds').val(selectedIds.join());
        $('#exportSelectedForm').submit();
    }

    e.preventDefault();
}

function importCsv(name) {
    var file = $('#importFile').get(0).files[0];
    var formData = new FormData();
    formData.append('file', file);
    $.ajax({
        url: '/dqms/upload',
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
                $("#importErrors").modal({backdrop: 'static'});
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
        url: '/dqms/report',
        data: formData,
        contentType: false,
        processData: false,
        success: function (data) {
            var reportRows = "<tbody id='reportTableBody'>";

            for (var i = 0; i < data.length; i++) {
                reportRows += "<tr><td>" + data[i].gmc + "</td><td>" + data[i].outstanding + "</td><td>" + data[i].resolved + "</td>"
                    + "<td>" + data[i].qtime + "</td><td>" + data[i].item + "</td><td>" + data[i].count + "</td></tr>";
            }
            reportRows += "</tbody>";

            $("#reportTableBody").replaceWith(reportRows);
            $("#reportsModal").modal({backdrop: 'static'});
            $("#reportsModal").modal('show');
        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred, please see server logs:" + textStatus + ': ' + errorThrown);
        }
    });
    return false;
}


function saveIssue() {
    var currentFilter = tableIssues.ajax.url();
    var formData = new FormData($('#addForm')[0]); //auto serialize
    formData.append("gmc", $("#addGMC").val());

    $.ajax({
        url: '/dqms/save',
        data: formData,
        type: 'POST',
        contentType: false,
        processData: false,
        success: function (data) {
            if (data == "Save ok") {
                BootstrapDialog.show({
                    title: 'Save Successful',
                    size: BootstrapDialog.SIZE_SMALL,
                    type: BootstrapDialog.TYPE_SUCCESS
                });
                tableIssues.ajax.url(currentFilter).load();
            } else {
                BootstrapDialog.show({
                    title: 'Save Failed',
                    message: data,
                    size: BootstrapDialog.SIZE_SMALL,
                    type: BootstrapDialog.TYPE_DANGER
                });
            }

        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred, please see server logs:" + textStatus + ': ' + errorThrown);
        }
    });
}

function updateIssue() {
    var currentFilter = tableIssues.ajax.url();
    var formData = new FormData($('#addForm')[0]); //auto serialize
    formData.append("gmc", $("#addGMC").val());

    $.ajax({
        url: '/dqms/update',
        data: formData,
        type: 'POST',
        contentType: false,
        processData: false,
        success: function (data) {
            if (data == "Update ok") {
                BootstrapDialog.show({
                    title: 'Update Successful',
                    size: BootstrapDialog.SIZE_SMALL,
                    type: BootstrapDialog.TYPE_SUCCESS
                });
                tableIssues.ajax.url(currentFilter).load();
            } else {
                BootstrapDialog.show({
                    title: 'Update Failed',
                    message: data,
                    size: BootstrapDialog.SIZE_SMALL,
                    type: BootstrapDialog.TYPE_DANGER
                });
            }

        },
        error: function (jqXHR, textStatus, errorThrown) {
            alert("An unexpected error occurred, please see server logs:" + textStatus + ': ' + errorThrown);
        }
    });
}

function bindAddFormValidation(isEdit) {
    var validator = $('#addForm').validate(
        {
            rules: {
                issueId: {
                    required: true
                },
                status: {
                    required: true
                },
                dateLogged: {
                    dateMask: true
                },
                participant: {
                    required: true,
                    digits: true,
                    range: [10000000, 999999999]
                },
                dataSource: {
                    required: true,
                    minlength: 2
                },
                priority: {
                    required: true,
                    digits: true,
                    range: [1, 9]
                },
                dataItem: {
                    required: true,
                    minlength: 2
                },
                shortDesc: {
                    required: true,
                    minlength: 2
                },
                gmc: {
                    required: true
                },
                lsid: {
                    minlength: 5
                },
                area: {
                    required: true,
                    therapeuticArea: true
                },
                familyId: {
                    minlength: 5
                },
                description: {
                    required: true,
                    minlength: 5
                }
            },
            highlight: function (element) {
                $(element).closest('.control-group').removeClass('valid').addClass('error');
            },
            unhighlight: function (element) {
                $(element).addClass('valid').removeClass('error')
                    .closest('.control-group').addClass('valid').removeClass('error');
            },
            submitHandler: function () {
                if ($("#addModalTitle").text().startsWith("Edit")) {
                    updateIssue();            //validator is bound to save button & kicks off saveIssue if validation passes
                } else {
                    saveIssue();            //validator is bound to save button & kicks off saveIssue if validation passes
                }
            }
        });

    $.validator.addMethod("therapeuticArea", function (value, element) {
        return this.optional(element) || /^(Cancer|RD)$/.test(value);
    }, "Please enter 'Cancer' or 'RD'");

    $.validator.addMethod("dateMask", function (value, element) {
        return this.optional(element) || /^\d{2}\/\d{2}\/\d{4}/i.test(value);
    }, "Date required");

    $('#addArea').on('keyup keypress', function (e) {
        if (e.key == "c" || e.key == "C") {
            $('#addArea').val("Cancer");
        }
        if (e.key == "r" || e.key == "R") {
            $('#addArea').val("RD");
        }
    });
    return validator;
}

function bindAddForm(isEdit) {
    var addFormValidr = bindAddFormValidation(isEdit);
    addFormValidr.resetForm();
    $("#addForm")[0].reset();

    var anySelectedRows = tableIssues.rows({selected: true}).count() > 0;
    //auto-populate from first selection
    if (anySelectedRows) {
        var found = false;
        tableIssues.rows({selected: true}).data().each(function (rowData) {
            if (!found) {
                $("#addDataSource").val(rowData.dataSource);
                $("#addDataItem").val(rowData.dataItem);
                $("#addShortDesc").val(rowData.shortDesc);
                $("#addDescription").val(rowData.description);
                $("#addNotes").val(rowData.notes);
                //copy all fields and make readOnly
                if(isEdit) {
                    $("#addIssueId").val(rowData.DT_RowId);
                    $("#addStatus").val(rowData.status);

                    var d =  moment(rowData.dateLogged,"DD-MMM-YYYY HH:mm:ss");
                    $("#addDateLogged").val(moment(d).format("DD/MM/YYYY HH:mm:ss")); //re-format to pass valdn

                    $("#addGMC").val(rowData.gmc);
                    $("#addParticipant").val(rowData.participantId);
                    $("#addPriority").val(rowData.priority);
                    $("#addLSID").val(rowData.lsid);
                    $("#addArea").val(rowData.area);
                    $("#addFamilyId").val(rowData.familyId);
                }
                found = true;
            }
        });
    }
    //populate 'volatile' fields on gmc select
    $("#addGMC").change(function () {
        var issueId = "null";
        var formData = new FormData();
        formData.append('gmc', this.value);
        if (this.value == "") {
            $("#addIssueId").val("");
            return;
        }
        $.ajax({
            url: '/dqms/nextIssueId',
            data: formData,
            type: 'POST',
            contentType: false,
            processData: false,
            success: function (data) {
                issueId = data;
                $("#addIssueId").val(issueId);
                $("#addDateLogged").val(moment().format("DD/MM/YYYY HH:mm:ss"));
                addFormValidr.form();
            },
            error: function (jqXHR, textStatus, errorThrown) {
                alert("An unexpected error occurred, please see server logs:" + textStatus + ': ' + errorThrown);
            }
        });
    });

    //only display edit if row selected
    if (isEdit && anySelectedRows) {
        $("#addModalTitle").text('Edit Issue Details');
        $("#addModalHeader").removeClass("ok-default");
        $("#addModalHeader").addClass("responded-default");

        // set the readOnlys
        $("#addGMC").prop('disabled', true);
        $("#addDataSource").prop('readonly', true);
        $("#addDataItem").prop('readonly', true);
        $("#addShortDesc").prop('readonly', true);
        $("#addDescription").prop('readonly', true);
        $("#addParticipant").prop('readonly', true);
        $("#addPriority").prop('readonly', true);
        $("#addLSID").prop('readonly', true);
        $("#addArea").prop('readonly', true);
        $("#addFamilyId").prop('readonly', true);

        $("#addModal").modal({backdrop: 'static'});
        $("#addModal").modal('show');
    }
    if (!isEdit) {  //it's 'add' then
        $("#addModalTitle").text('New Issue Details');
        $("#addModalHeader").removeClass("responded-default");
        $("#addModalHeader").addClass("ok-default");

        // reset the readOnlys
        $("#addGMC").prop('disabled', false);
        $("#addDataSource").prop('readonly', false);
        $("#addDataItem").prop('readonly', false);
        $("#addShortDesc").prop('readonly', false);
        $("#addDescription").prop('readonly', false);
        $("#addParticipant").prop('readonly', false);
        $("#addPriority").prop('readonly', false);
        $("#addLSID").prop('readonly', false);
        $("#addArea").prop('readonly', false);
        $("#addFamilyId").prop('readonly', false);

        $("#addModal").modal({backdrop: 'static'});
        $("#addModal").modal('show');
    }
}

//import event setup
$(function () {
    // attach `fileselect` event to all file inputs
    $(document).on('change', ':file', function () {
        var input = $(this),
            numFiles = input.get(0).files ? input.get(0).files.length : 1,
            label = input.val().replace(/\\/g, '/').replace(/.*\//, '');
        input.trigger('fileselect', [numFiles, label]);
    });

    // watch for custom `fileselect` event
    $(document).ready(function () {
        $(':file').on('fileselect', function (event, numFiles, label) {

            var input = $(this).parents('.input-group').find(':text'),
                selectedFile = numFiles > 1 ? numFiles + ' files selected' : label;

            if (input.length) {
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
    loadPriorities();
    resetInputs();

    $("#allIssues").click(function (e) {
        resetInputs();
        resetExportButton();
        var url = "/dqms/list?" + buildFilter();
        filterTable(tableIssues, "/dqms/list", e);
    });

    $("#newIssues").click(function (e) {
        resetInputs();
        $('#statusSelect').val('Open');
        $('#startDate').datepicker('update', moment().subtract(7, 'days').format("DD/MM/YYYY"));

        //var url = "/list?filter=new&days=30";   //should be last 30days by default
        var url = "/dqms/list?" + buildFilter();
        filterTable(tableIssues, url, e);
    });

    $('#nav').on('change', '#gmcSelect, #statusSelect, #originSelect, #prioritySelect, #areaSelect', function (e) {
        var url = "/dqms/list?" + buildFilter();
        filterTable(tableIssues, url, e);
    });

    $('#nav').on('change', '#startDate, #endDate', function (e) {
        applyFilterHighlights();
    });

    $('#nav').on('changeDate blur', '#startDate, #endDate', function (e) {
        var url = "/dqms/list?" + buildFilter();
        filterTable(tableIssues, url, e);
    });

    $("#addButton").click(function (e) {
        bindAddForm(false);
    });

    $("#editButton").click(function (e) {
        bindAddForm(true);
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
        var anySelectedRows = tableIssues.rows({selected: true}).count() > 0;
        if (anySelectedRows) {
            exportSelected(e);
        } else {
            exportCsv();
        }
    });


    $("#proceedImport").click(function (e) {
        var selectedFile = $("#importFile").get(0).files[0].name;
        if (selectedFile.length) {
            importCsv(selectedFile);
        }
    });


    $('#main').on('click', '.select-checkbox', function (e) {
        var anySelectedRows = tableIssues.rows({selected: true}).count() > 0;
        if (anySelectedRows) {
            $("#export").text("Export selected");
        } else {
            resetExportButton();
        }
    });


    $("#addForm").submit(function (e) {
        e.preventDefault();
    });

    //Qchain
    //$(document ).on( "click", "TD.qchain", function() {
    //    displayQChain(this);
    //});

    $('.datepicker').datepicker({
        format: 'dd/mm/yyyy',
        todayBtn: "linked",
        language: "en-GB",
        autoclose: true,
        todayHighlight: true
    });

});


