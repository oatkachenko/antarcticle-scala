jQuery(=>
  $(document).ready(=>
    $('pre > code').each (i, el) =>
      # language specific class
      lang = $(el).attr('class')
      # add classes for highlighting and line numbers
      $(el).parent().addClass('prettyprint').addClass('linenums').addClass("lang-#{lang}")

    $('a#delete-article').click =>
      bootbox.confirm("Are you sure? This operation cannot be undone", (result) =>
         document.location = $('a#delete-article').attr('href') if result
      )
      return false

    $('#comment_submit').click =>
      $.post($('#comment_form').attr('action'),
        {articleId: $('#article_id').val(), content: $('#comment_content').val() })
          .done( => location.reload(true))
          .fail( => alert('Error'))
      return false

    # turn on highlighting
    prettyPrint()
    # custom tags input field
    $('#tags_filter').tags_input()
  )
)

