# Helper class to ease AJAX validation on forms.
# Every form with, suitable for selector given is processed. Regular submit is replaced
# with AJAX call to form's action URL.
#
# Default selector: class 'default-form'
# Default OK action: redirect to URL from server response body
# Default validation fail action: prepend server response content to the form, i.e. response should contain
# error description and markup. See also formErrors.scala.html template
#
# Selector and actions are customizable, just create your own FormHandler instance with
# all necessary stuff set as constructor parameters.

class FormHandler

  defaultOnSuccess: (data) =>
    $("body").css("cursor", "default")
    window.location.href = data
    if (window.location.href.split('#')[0] == data.split('#')[0])
      window.location.reload(true)

  defaultOnFail: (data) =>
    $('.clear-on-resubmit').remove() # clear old validation messages, if any
    if (data.status == 413)
      # it seems there's no easy way to handle http 413 globally on server
      this.form.prepend("<div class='alert alert-danger form-error clear-on-resubmit'>" +
      "<a class='close' data-dismiss='alert' href='#'>x</a>Operation cannot be completed: too much data sent</div>")
    else
      this.form.prepend(data.responseText)
    $('.default-form input[type="submit"]').prop('disabled', false);
    $("body").css("cursor", "default")

  constructor: (@selector, @onSuccess = this.defaultOnSuccess, @onFail = this.defaultOnFail) ->
    this.form = $(@selector)
    this.form.submit(=>
      $("body").css("cursor", "progress")
      $('.default-form input[type="submit"]').prop('disabled', true);
      $.post(this.form.attr('action'), this.form.serialize())
      .done((data) => @onSuccess(data))
      .fail((data) => @onFail(data))
      return false
    )


new FormHandler('.default-form')

