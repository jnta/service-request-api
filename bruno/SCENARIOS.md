# HTTP acceptance scenarios

| Round | Request | Expected HTTP |
| --- | --- | --- |
| 01-setup | Initialize run | 200 |
| 01-setup | Register client | 201 |
| 01-setup | Login client | 200 |
| 01-setup | Register other | 201 |
| 01-setup | Login other | 200 |
| 01-setup | Login admin | 200 |
| 01-setup | Create analyst | 201 |
| 01-setup | Login analyst | 200 |
| 01-setup | Create outside | 201 |
| 01-setup | Login outside | 200 |
| 02-round-1-happy-path | Create approved draft | 201 |
| 02-round-1-happy-path | Read approved draft | 200 |
| 02-round-1-happy-path | Save approved basic data | 200 |
| 02-round-1-happy-path | Resolve approved address | 200 |
| 02-round-1-happy-path | Confirm approved | 200 |
| 02-round-1-happy-path | Submit approved | 200 |
| 02-round-1-happy-path | Find approved in search | 200 |
| 02-round-1-happy-path | Start approved review | 200 |
| 02-round-1-happy-path | Decide approved | 200 |
| 02-round-1-happy-path | Read approved terminal state | 200 |
| 02-round-1-happy-path | Create rejected draft | 201 |
| 02-round-1-happy-path | Read rejected draft | 200 |
| 02-round-1-happy-path | Save rejected basic data | 200 |
| 02-round-1-happy-path | Resolve rejected address | 200 |
| 02-round-1-happy-path | Confirm rejected | 200 |
| 02-round-1-happy-path | Submit rejected | 200 |
| 02-round-1-happy-path | Find rejected in search | 200 |
| 02-round-1-happy-path | Start rejected review | 200 |
| 02-round-1-happy-path | Decide rejected | 200 |
| 02-round-1-happy-path | Read rejected terminal state | 200 |
| 03-round-2-negative-security | Missing authentication | 401 |
| 03-round-2-negative-security | Invalid bearer token | 401 |
| 03-round-2-negative-security | Wrong password | 401 |
| 03-round-2-negative-security | Duplicate email | 409 |
| 03-round-2-negative-security | Short password | 400 |
| 03-round-2-negative-security | Malformed JSON | 400 |
| 03-round-2-negative-security | Client cannot provision users | 403 |
| 03-round-2-negative-security | Client cannot search | 403 |
| 03-round-2-negative-security | Analyst cannot create client draft | 403 |
| 03-round-2-negative-security | Other client cannot read | 403 |
| 03-round-2-negative-security | Other client cannot edit | 403 |
| 03-round-2-negative-security | Other client cannot submit | 403 |
| 03-round-2-negative-security | Outside analyst cannot read | 403 |
| 03-round-2-negative-security | Outside analyst cannot start | 403 |
| 03-round-2-negative-security | Outside analyst cannot decide | 403 |
| 03-round-2-negative-security | Search cannot expand coverage | 200 |
| 03-round-2-negative-security | Unknown solicitation | 404 |
| 03-round-2-negative-security | Cannot edit approved | 409 |
| 03-round-2-negative-security | Cannot resubmit approved | 409 |
| 03-round-2-negative-security | Cannot edit rejected | 409 |
| 03-round-2-negative-security | Cannot reopen terminal review | 409 |
| 03-round-2-negative-security | Cannot decide twice | 409 |
| 03-round-2-negative-security | Invalid search pagination | 400 |
| 03-round-2-negative-security | Invalid search sort | 400 |
| 03-round-2-negative-security | Create validation draft | 201 |
| 03-round-2-negative-security | Cannot skip basic step | 409 |
| 03-round-2-negative-security | Cannot skip address step | 409 |
| 03-round-2-negative-security | Cannot submit incomplete draft | 422 |
| 03-round-2-negative-security | Incomplete draft unchanged | 200 |
| 03-round-2-negative-security | Invalid basic fields | 400 |
| 03-round-2-negative-security | Valid basic fixture | 200 |
| 03-round-2-negative-security | Malformed CEP | 400 |
| 03-round-2-negative-security | Unknown CEP | 422 |
| 03-round-2-negative-security | Invalid address unchanged | 200 |
| 03-round-2-negative-security | Valid address fixture | 200 |
| 03-round-2-negative-security | Past preferred date | 422 |
| 03-round-2-negative-security | High priority below minimum | 422 |
| 03-round-2-negative-security | Negative amount | 422 |
| 03-round-2-negative-security | Excess monetary precision | 422 |
| 03-round-2-negative-security | Unsupported currency | 400 |
| 03-round-2-negative-security | Missing confirmation fields | 400 |
| 03-round-2-negative-security | Invalid confirmation unchanged | 200 |
| 03-round-2-negative-security | Save declined terms | 200 |
| 03-round-2-negative-security | Cannot submit declined terms | 422 |
| 03-round-2-negative-security | Declined terms draft unchanged | 200 |
| 03-round-2-negative-security | High priority boundary accepted | 200 |
| 03-round-2-negative-security | Cannot decide draft | 409 |
| 03-round-2-negative-security | Cannot review draft | 409 |
| 03-round-2-negative-security | Submit validation fixture | 200 |
| 03-round-2-negative-security | Cannot decide before review | 409 |
| 03-round-2-negative-security | Start validation review | 200 |
| 03-round-2-negative-security | Blank decision comment | 400 |
| 03-round-2-negative-security | Invalid decision unchanged | 200 |
| 03-round-2-negative-security | Finish validation fixture | 200 |
