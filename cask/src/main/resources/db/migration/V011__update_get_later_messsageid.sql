/*
 *
 * Purpose: To fetch the message id with a more recent insertedat value.
 * For view 'find' statements where we need to left join two cask tables, we have two cask message ids to choose from.
 * This function enables us to select the message id with more recent insertion date.
 * 'insertedAt' important as
 * findAll { Foo[] } (CaskInsertedAt < t1, CasekInsertedAt > t2)
 * query is resolved through:
 * SELECT FOO_TABLE.*, cask_message insertedAt FROM FOO_TABLE INNER JOIN ON FOO_TABLE.messageId = cask_message.messageid
 *   WHERE insertedAt < t1 AND insertedAt > t2
 * For View based types where use LEFT JOIN, selection of an arbitrary messageId would case duplicates in CaskInsertedAt queries.
 * This function resolves this problem by returning the most recent cask message id.
 */
CREATE OR REPLACE FUNCTION public.get_later_messsageid(messageid1 character varying, messageid2 character varying)
    RETURNS character varying
    LANGUAGE plpgsql
AS $function$
Declare
    resultMessageId varchar(40);
    message1timestamp timestamp;
    message2timestamp timestamp;
Begin
    /* If either of the messageids are null return the one which is not null */
    if messageid1 is null AND messageid2 is not null then
        return messageid2;
    end if;

    if messageid1 is not null AND messageid2 is null then
        return messageid1;
    end if;

    select insertedat from cask_message where id = messageId1 into message1timestamp;
    select insertedat from cask_message where id = messageId2 into message2timestamp;
    if  message1timestamp > message2timestamp then
        select messageId1 into resultMessageId;
    else
        select messageId2 into resultMessageId;
    end if;
    return  resultMessageId;
End;
$function$
