-- Wrap page TEXT properties that are used as titles inside H1 elements

UPDATE
    pageproperty
SET
    value = CONCAT('<h1 class="md">', value, '</h1>')
WHERE
    propertyKey in (SELECT variablekey FROM layoutvariable WHERE variabletype = 'TEXT') AND
    propertyKey in (
        SELECT
        REPLACE(REPLACE(REGEXP_SUBSTR(html, '<h1 id=''[0-9a-f\-]+'''), '<h1 id=', ''), "'", '')
        FROM
        layout
        WHERE
        html
        REGEXP '<h1 id=''[0-9a-f\-]+'''
    );

-- Wrap page TEXT properties that are used as text inside P elements

UPDATE
    pageproperty
SET
    value = CONCAT('<p>', value, '</p>')
WHERE
    propertyKey in (SELECT variablekey FROM layoutvariable WHERE variabletype = 'TEXT') AND
    propertyKey not in (
        SELECT
        REPLACE(REPLACE(REGEXP_SUBSTR(html, '<h1 id=''[0-9a-f\-]+'''), '<h1 id=', ''), "'", '')
        FROM
        layout
        WHERE
        html
        REGEXP '<h1 id=''[0-9a-f\-]+'''
    );

-- Wrap h1 page titles inside header-container divs

UPDATE
    layout
SET
    html = REGEXP_REPLACE(html, '<h1 id=''([^'']*)'' class=''title md''>(.*?)</h1>', '<div id=''$1'' data-component=''header-container''><h1 class=''title md''>$2</h1></div>')
WHERE
    html like '%<h1%';

-- Wrap p page text inside text-container divs

UPDATE
    layout
SET
    html = REGEXP_REPLACE(html, '<p id=''([^'']*)''>(.*?)</p>', '<div data-component=''text-container'' id=''$1''>\n  <p>$2</p>\n</div>')
WHERE
    html like '%<p%';

-- Wrap question options inside divs

UPDATE
    questionoption
SET
    value = CONCAT('<div>', value, '</div>');