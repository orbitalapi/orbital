
--
-- Name: actor_actor_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE actor_actor_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;


--
--

CREATE TABLE actor (
                      actor_id integer DEFAULT nextval('actor_actor_id_seq'::regclass) NOT NULL,
                      first_name character varying(45) NOT NULL,
                      last_name character varying(45) NOT NULL,
                      last_update timestamp without time zone DEFAULT now() NOT NULL
);


--
--

CREATE TYPE mpaa_rating AS ENUM (
   'G',
   'PG',
   'PG-13',
   'R',
   'NC-17'
   );


--
--

CREATE DOMAIN year AS integer
   CONSTRAINT year_check CHECK (((VALUE >= 1901) AND (VALUE <= 2155)));


--
--


--
--


--
--

CREATE SEQUENCE category_category_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE category (
                         category_id integer DEFAULT nextval('category_category_id_seq'::regclass) NOT NULL,
                         name character varying(25) NOT NULL,
                         last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--

CREATE SEQUENCE film_film_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE film (
                     film_id integer DEFAULT nextval('film_film_id_seq'::regclass) NOT NULL,
                     title character varying(255) NOT NULL,
                     description text,
                     release_year year,
                     language_id smallint NOT NULL,
                     original_language_id smallint,
                     rental_duration smallint DEFAULT 3 NOT NULL,
                     rental_rate numeric(4,2) DEFAULT 4.99 NOT NULL,
                     length smallint,
                     replacement_cost numeric(5,2) DEFAULT 19.99 NOT NULL,
                     rating mpaa_rating DEFAULT 'G'::mpaa_rating,
                     last_update timestamp without time zone DEFAULT now() NOT NULL,
                     special_features text[],
                     fulltext tsvector NOT NULL
);



--
--

CREATE TABLE film_actor (
                           actor_id smallint NOT NULL,
                           film_id smallint NOT NULL,
                           last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--

CREATE TABLE film_category (
                              film_id smallint NOT NULL,
                              category_id smallint NOT NULL,
                              last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--


--
--

CREATE SEQUENCE address_address_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE address (
                        address_id integer DEFAULT nextval('address_address_id_seq'::regclass) NOT NULL,
                        address character varying(50) NOT NULL,
                        address2 character varying(50),
                        district character varying(20) NOT NULL,
                        city_id smallint NOT NULL,
                        postal_code character varying(10),
                        phone character varying(20) NOT NULL,
                        last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--

CREATE SEQUENCE city_city_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE city (
                     city_id integer DEFAULT nextval('city_city_id_seq'::regclass) NOT NULL,
                     city character varying(50) NOT NULL,
                     country_id smallint NOT NULL,
                     last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--

CREATE SEQUENCE country_country_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE country (
                        country_id integer DEFAULT nextval('country_country_id_seq'::regclass) NOT NULL,
                        country character varying(50) NOT NULL,
                        last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--

CREATE SEQUENCE customer_customer_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE customer (
                         customer_id integer DEFAULT nextval('customer_customer_id_seq'::regclass) NOT NULL,
                         store_id smallint NOT NULL,
                         first_name character varying(45) NOT NULL,
                         last_name character varying(45) NOT NULL,
                         email character varying(50),
                         address_id smallint NOT NULL,
                         activebool boolean DEFAULT true NOT NULL,
                         create_date date DEFAULT ('now'::text)::date NOT NULL,
                         last_update timestamp without time zone DEFAULT now(),
                         active integer
);



--
--



--
--

--
--

CREATE SEQUENCE inventory_inventory_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE inventory (
                          inventory_id integer DEFAULT nextval('inventory_inventory_id_seq'::regclass) NOT NULL,
                          film_id smallint NOT NULL,
                          store_id smallint NOT NULL,
                          last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--

CREATE SEQUENCE language_language_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE language (
                         language_id integer DEFAULT nextval('language_language_id_seq'::regclass) NOT NULL,
                         name character(20) NOT NULL,
                         last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--


--
--

CREATE SEQUENCE payment_payment_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE payment (
                        payment_id integer DEFAULT nextval('payment_payment_id_seq'::regclass) NOT NULL,
                        customer_id smallint NOT NULL,
                        staff_id smallint NOT NULL,
                        rental_id integer NOT NULL,
                        amount numeric(5,2) NOT NULL,
                        payment_date timestamp without time zone NOT NULL
);



--
--

CREATE TABLE payment_p2007_01 (CONSTRAINT payment_p2007_01_payment_date_check CHECK (((payment_date >= '2007-01-01 00:00:00'::timestamp without time zone) AND (payment_date < '2007-02-01 00:00:00'::timestamp without time zone)))
)
   INHERITS (payment);



--
--

CREATE TABLE payment_p2007_02 (CONSTRAINT payment_p2007_02_payment_date_check CHECK (((payment_date >= '2007-02-01 00:00:00'::timestamp without time zone) AND (payment_date < '2007-03-01 00:00:00'::timestamp without time zone)))
)
   INHERITS (payment);



--
--

CREATE TABLE payment_p2007_03 (CONSTRAINT payment_p2007_03_payment_date_check CHECK (((payment_date >= '2007-03-01 00:00:00'::timestamp without time zone) AND (payment_date < '2007-04-01 00:00:00'::timestamp without time zone)))
)
   INHERITS (payment);



--
--

CREATE TABLE payment_p2007_04 (CONSTRAINT payment_p2007_04_payment_date_check CHECK (((payment_date >= '2007-04-01 00:00:00'::timestamp without time zone) AND (payment_date < '2007-05-01 00:00:00'::timestamp without time zone)))
)
   INHERITS (payment);



--
--

CREATE TABLE payment_p2007_05 (CONSTRAINT payment_p2007_05_payment_date_check CHECK (((payment_date >= '2007-05-01 00:00:00'::timestamp without time zone) AND (payment_date < '2007-06-01 00:00:00'::timestamp without time zone)))
)
   INHERITS (payment);



--
--

CREATE TABLE payment_p2007_06 (CONSTRAINT payment_p2007_06_payment_date_check CHECK (((payment_date >= '2007-06-01 00:00:00'::timestamp without time zone) AND (payment_date < '2007-07-01 00:00:00'::timestamp without time zone)))
)
   INHERITS (payment);



--
--

CREATE SEQUENCE rental_rental_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE rental (
                       rental_id integer DEFAULT nextval('rental_rental_id_seq'::regclass) NOT NULL,
                       rental_date timestamp without time zone NOT NULL,
                       inventory_id integer NOT NULL,
                       customer_id smallint NOT NULL,
                       return_date timestamp without time zone,
                       staff_id smallint NOT NULL,
                       last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--


--
--

CREATE SEQUENCE staff_staff_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE staff (
                      staff_id integer DEFAULT nextval('staff_staff_id_seq'::regclass) NOT NULL,
                      first_name character varying(45) NOT NULL,
                      last_name character varying(45) NOT NULL,
                      address_id smallint NOT NULL,
                      email character varying(50),
                      store_id smallint NOT NULL,
                      active boolean DEFAULT true NOT NULL,
                      username character varying(16) NOT NULL,
                      password character varying(40),
                      last_update timestamp without time zone DEFAULT now() NOT NULL,
                      picture bytea
);



--
--

CREATE SEQUENCE store_store_id_seq
   INCREMENT BY 1
   NO MAXVALUE
   NO MINVALUE
   CACHE 1;



--
--

CREATE TABLE store (
                      store_id integer DEFAULT nextval('store_store_id_seq'::regclass) NOT NULL,
                      manager_staff_id smallint NOT NULL,
                      address_id smallint NOT NULL,
                      last_update timestamp without time zone DEFAULT now() NOT NULL
);



--
--




--
--




--
--



ALTER TABLE ONLY actor
   ADD CONSTRAINT actor_pkey PRIMARY KEY (actor_id);


--
--

ALTER TABLE ONLY address
   ADD CONSTRAINT address_pkey PRIMARY KEY (address_id);


--
--

ALTER TABLE ONLY category
   ADD CONSTRAINT category_pkey PRIMARY KEY (category_id);


--
--

ALTER TABLE ONLY city
   ADD CONSTRAINT city_pkey PRIMARY KEY (city_id);


--
--

ALTER TABLE ONLY country
   ADD CONSTRAINT country_pkey PRIMARY KEY (country_id);


--
--

ALTER TABLE ONLY customer
   ADD CONSTRAINT customer_pkey PRIMARY KEY (customer_id);


--
--

ALTER TABLE ONLY film_actor
   ADD CONSTRAINT film_actor_pkey PRIMARY KEY (actor_id, film_id);


--
--

ALTER TABLE ONLY film_category
   ADD CONSTRAINT film_category_pkey PRIMARY KEY (film_id, category_id);


--
--

ALTER TABLE ONLY film
   ADD CONSTRAINT film_pkey PRIMARY KEY (film_id);


--
--

ALTER TABLE ONLY inventory
   ADD CONSTRAINT inventory_pkey PRIMARY KEY (inventory_id);


--
--

ALTER TABLE ONLY language
   ADD CONSTRAINT language_pkey PRIMARY KEY (language_id);


--
--

ALTER TABLE ONLY payment
   ADD CONSTRAINT payment_pkey PRIMARY KEY (payment_id);


--
--

ALTER TABLE ONLY rental
   ADD CONSTRAINT rental_pkey PRIMARY KEY (rental_id);


--
--

ALTER TABLE ONLY staff
   ADD CONSTRAINT staff_pkey PRIMARY KEY (staff_id);


--
--

ALTER TABLE ONLY store
   ADD CONSTRAINT store_pkey PRIMARY KEY (store_id);


--
--

CREATE INDEX film_fulltext_idx ON film USING gist (fulltext);


--
--

CREATE INDEX idx_actor_last_name ON actor USING btree (last_name);


--
--

CREATE INDEX idx_fk_address_id ON customer USING btree (address_id);


--
--

CREATE INDEX idx_fk_city_id ON address USING btree (city_id);


--
--

CREATE INDEX idx_fk_country_id ON city USING btree (country_id);


--
--

CREATE INDEX idx_fk_customer_id ON payment USING btree (customer_id);


--
--

CREATE INDEX idx_fk_film_id ON film_actor USING btree (film_id);


--
--

CREATE INDEX idx_fk_inventory_id ON rental USING btree (inventory_id);


--
--

CREATE INDEX idx_fk_language_id ON film USING btree (language_id);


--
--

CREATE INDEX idx_fk_original_language_id ON film USING btree (original_language_id);


--
--

CREATE INDEX idx_fk_payment_p2007_01_customer_id ON payment_p2007_01 USING btree (customer_id);


--
--

CREATE INDEX idx_fk_payment_p2007_01_staff_id ON payment_p2007_01 USING btree (staff_id);


--
--

CREATE INDEX idx_fk_payment_p2007_02_customer_id ON payment_p2007_02 USING btree (customer_id);


--
--

CREATE INDEX idx_fk_payment_p2007_02_staff_id ON payment_p2007_02 USING btree (staff_id);


--
--

CREATE INDEX idx_fk_payment_p2007_03_customer_id ON payment_p2007_03 USING btree (customer_id);


--
--

CREATE INDEX idx_fk_payment_p2007_03_staff_id ON payment_p2007_03 USING btree (staff_id);


--
--

CREATE INDEX idx_fk_payment_p2007_04_customer_id ON payment_p2007_04 USING btree (customer_id);


--
--

CREATE INDEX idx_fk_payment_p2007_04_staff_id ON payment_p2007_04 USING btree (staff_id);


--
--

CREATE INDEX idx_fk_payment_p2007_05_customer_id ON payment_p2007_05 USING btree (customer_id);


--
--

CREATE INDEX idx_fk_payment_p2007_05_staff_id ON payment_p2007_05 USING btree (staff_id);


--
--

CREATE INDEX idx_fk_payment_p2007_06_customer_id ON payment_p2007_06 USING btree (customer_id);


--
--

CREATE INDEX idx_fk_payment_p2007_06_staff_id ON payment_p2007_06 USING btree (staff_id);


--
--

CREATE INDEX idx_fk_staff_id ON payment USING btree (staff_id);


--
--

CREATE INDEX idx_fk_store_id ON customer USING btree (store_id);


--
--

CREATE INDEX idx_last_name ON customer USING btree (last_name);


--
--

CREATE INDEX idx_store_id_film_id ON inventory USING btree (store_id, film_id);


--
--

CREATE INDEX idx_title ON film USING btree (title);


--
--

CREATE UNIQUE INDEX idx_unq_manager_staff_id ON store USING btree (manager_staff_id);


--
--

CREATE UNIQUE INDEX idx_unq_rental_rental_date_inventory_id_customer_id ON rental USING btree (rental_date, inventory_id, customer_id);


--
--

ALTER TABLE ONLY address
   ADD CONSTRAINT address_city_id_fkey FOREIGN KEY (city_id) REFERENCES city(city_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: city_country_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY city
   ADD CONSTRAINT city_country_id_fkey FOREIGN KEY (country_id) REFERENCES country(country_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: customer_address_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY customer
   ADD CONSTRAINT customer_address_id_fkey FOREIGN KEY (address_id) REFERENCES address(address_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: customer_store_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY customer
   ADD CONSTRAINT customer_store_id_fkey FOREIGN KEY (store_id) REFERENCES store(store_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: film_actor_actor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY film_actor
   ADD CONSTRAINT film_actor_actor_id_fkey FOREIGN KEY (actor_id) REFERENCES actor(actor_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: film_actor_film_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY film_actor
   ADD CONSTRAINT film_actor_film_id_fkey FOREIGN KEY (film_id) REFERENCES film(film_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: film_category_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY film_category
   ADD CONSTRAINT film_category_category_id_fkey FOREIGN KEY (category_id) REFERENCES category(category_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: film_category_film_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY film_category
   ADD CONSTRAINT film_category_film_id_fkey FOREIGN KEY (film_id) REFERENCES film(film_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: film_language_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY film
   ADD CONSTRAINT film_language_id_fkey FOREIGN KEY (language_id) REFERENCES language(language_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: film_original_language_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY film
   ADD CONSTRAINT film_original_language_id_fkey FOREIGN KEY (original_language_id) REFERENCES language(language_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: inventory_film_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY inventory
   ADD CONSTRAINT inventory_film_id_fkey FOREIGN KEY (film_id) REFERENCES film(film_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: inventory_store_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY inventory
   ADD CONSTRAINT inventory_store_id_fkey FOREIGN KEY (store_id) REFERENCES store(store_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: payment_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment
   ADD CONSTRAINT payment_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: payment_p2007_01_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_01
   ADD CONSTRAINT payment_p2007_01_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customer(customer_id);


--
-- Name: payment_p2007_01_rental_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_01
   ADD CONSTRAINT payment_p2007_01_rental_id_fkey FOREIGN KEY (rental_id) REFERENCES rental(rental_id);


--
-- Name: payment_p2007_01_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_01
   ADD CONSTRAINT payment_p2007_01_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES staff(staff_id);


--
-- Name: payment_p2007_02_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_02
   ADD CONSTRAINT payment_p2007_02_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customer(customer_id);


--
-- Name: payment_p2007_02_rental_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_02
   ADD CONSTRAINT payment_p2007_02_rental_id_fkey FOREIGN KEY (rental_id) REFERENCES rental(rental_id);


--
-- Name: payment_p2007_02_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_02
   ADD CONSTRAINT payment_p2007_02_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES staff(staff_id);


--
-- Name: payment_p2007_03_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_03
   ADD CONSTRAINT payment_p2007_03_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customer(customer_id);


--
-- Name: payment_p2007_03_rental_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_03
   ADD CONSTRAINT payment_p2007_03_rental_id_fkey FOREIGN KEY (rental_id) REFERENCES rental(rental_id);


--
-- Name: payment_p2007_03_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_03
   ADD CONSTRAINT payment_p2007_03_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES staff(staff_id);


--
-- Name: payment_p2007_04_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_04
   ADD CONSTRAINT payment_p2007_04_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customer(customer_id);


--
-- Name: payment_p2007_04_rental_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_04
   ADD CONSTRAINT payment_p2007_04_rental_id_fkey FOREIGN KEY (rental_id) REFERENCES rental(rental_id);


--
-- Name: payment_p2007_04_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_04
   ADD CONSTRAINT payment_p2007_04_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES staff(staff_id);


--
-- Name: payment_p2007_05_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_05
   ADD CONSTRAINT payment_p2007_05_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customer(customer_id);


--
-- Name: payment_p2007_05_rental_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_05
   ADD CONSTRAINT payment_p2007_05_rental_id_fkey FOREIGN KEY (rental_id) REFERENCES rental(rental_id);


--
-- Name: payment_p2007_05_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_05
   ADD CONSTRAINT payment_p2007_05_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES staff(staff_id);


--
-- Name: payment_p2007_06_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_06
   ADD CONSTRAINT payment_p2007_06_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customer(customer_id);


--
-- Name: payment_p2007_06_rental_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_06
   ADD CONSTRAINT payment_p2007_06_rental_id_fkey FOREIGN KEY (rental_id) REFERENCES rental(rental_id);


--
-- Name: payment_p2007_06_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment_p2007_06
   ADD CONSTRAINT payment_p2007_06_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES staff(staff_id);


--
-- Name: payment_rental_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment
   ADD CONSTRAINT payment_rental_id_fkey FOREIGN KEY (rental_id) REFERENCES rental(rental_id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: payment_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY payment
   ADD CONSTRAINT payment_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES staff(staff_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: rental_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY rental
   ADD CONSTRAINT rental_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: rental_inventory_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY rental
   ADD CONSTRAINT rental_inventory_id_fkey FOREIGN KEY (inventory_id) REFERENCES inventory(inventory_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: rental_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY rental
   ADD CONSTRAINT rental_staff_id_fkey FOREIGN KEY (staff_id) REFERENCES staff(staff_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: staff_address_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY staff
   ADD CONSTRAINT staff_address_id_fkey FOREIGN KEY (address_id) REFERENCES address(address_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: staff_store_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY staff
   ADD CONSTRAINT staff_store_id_fkey FOREIGN KEY (store_id) REFERENCES store(store_id);


--
-- Name: store_address_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY store
   ADD CONSTRAINT store_address_id_fkey FOREIGN KEY (address_id) REFERENCES address(address_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: store_manager_staff_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY store
   ADD CONSTRAINT store_manager_staff_id_fkey FOREIGN KEY (manager_staff_id) REFERENCES staff(staff_id) ON UPDATE CASCADE ON DELETE RESTRICT;

