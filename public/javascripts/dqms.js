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
        ]//,
        //"columnDefs": [
        //    {
        //        "targets": [3],
        //        "type" : "date",
        //        "render": function (data) {
        //            if (data !== null) {
        //                var javascriptDate = new Date(data);
        //                //console.log(data);
        //                //javascriptDate = javascriptDate.getMonth() + 1 + "/" + javascriptDate.getDate() + "/" + javascriptDate.getFullYear();
        //                return javascriptDate;
        //            } else {
        //                return "";
        //            }
        //        }
        //    }
        //]
    });
}

$(document).ready(function() {
    loadIssues();

    var tableIssues = $('#issuesTable').DataTable();
});


