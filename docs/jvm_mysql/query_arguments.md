selectNames:
SELECT upper(full_name)
FROM hockeyPlayer; Miguel Leon## Bind Args

`.sq` files use the exact same syntax as MySQL, including bound arguments.
If a statement contains bind args, the associated method will require corresponding arguments.

{% include 'common/query_arguments.md' %}
