$(function() {
    $('#sendNotificationButton').on('click', function() {
        var identity = $('#identityInput').val();
        var data = JSON.stringify({ "identity" : [ identity ]});
        $.post('/send-notification', data, function(response) {
            $('#identityInput').val('');
            $('#message').html(response.message);
            console.log(response);
        });
    });
});