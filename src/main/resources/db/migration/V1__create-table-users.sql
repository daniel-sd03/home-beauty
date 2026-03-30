-- 1. Tabelas Base (Não dependem de nenhuma outra tabela)
CREATE TABLE users (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    name TEXT,
    email TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    phone TEXT,
    role TEXT NOT NULL,
    dt_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE states (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    name TEXT NOT NULL,
    uf VARCHAR(2) NOT NULL
);

CREATE TABLE categories (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    name TEXT NOT NULL
);

-- 2. Tabelas de Primeiro Nível de Dependência
CREATE TABLE cities (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,    
    state_id TEXT NOT NULL,
    name TEXT NOT NULL,
    CONSTRAINT fk_city_state FOREIGN KEY (state_id) REFERENCES states(id)
);

CREATE TABLE professional_profiles (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    user_id TEXT NOT NULL,
    description TEXT,
    home_service BOOLEAN DEFAULT FALSE,
    average_rating DECIMAL(3, 2) DEFAULT 0.00,
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 3. Tabelas de Segundo Nível de Dependência
CREATE TABLE addresses (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    user_id TEXT NOT NULL,
    city_id TEXT NOT NULL,
    street TEXT NOT NULL,
    number TEXT,
    complement TEXT,
    neighborhood TEXT NOT NULL,
    zip_code TEXT NOT NULL,
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    CONSTRAINT fk_address_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_address_city FOREIGN KEY (city_id) REFERENCES cities(id)
);

CREATE TABLE working_hours (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    professional_id TEXT NOT NULL,
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    CONSTRAINT fk_working_hours_professional FOREIGN KEY (professional_id) REFERENCES professional_profiles(id)
);

CREATE TABLE services (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    professional_id TEXT NOT NULL,
    category_id TEXT NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    duration_minutes INTEGER NOT NULL,
    CONSTRAINT fk_service_professional FOREIGN KEY (professional_id) REFERENCES professional_profiles(id),
    CONSTRAINT fk_service_category FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- 4. Tabela de Agendamentos (Depende de quase tudo)
CREATE TABLE appointments (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    client_id TEXT NOT NULL,
    professional_id TEXT NOT NULL,
    service_id TEXT NOT NULL,
    address_id TEXT,
    appointment_type TEXT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status TEXT NOT NULL,
    notes TEXT,
    CONSTRAINT fk_appointment_client FOREIGN KEY (client_id) REFERENCES users(id),
    CONSTRAINT fk_appointment_professional FOREIGN KEY (professional_id) REFERENCES professional_profiles(id),
    CONSTRAINT fk_appointment_service FOREIGN KEY (service_id) REFERENCES services(id),
    CONSTRAINT fk_appointment_address FOREIGN KEY (address_id) REFERENCES addresses(id)
);

-- 5. Tabela de Avaliações (Depende dos Agendamentos)
CREATE TABLE reviews (
    id TEXT PRIMARY KEY UNIQUE NOT NULL,
    appointment_id TEXT NOT NULL,
    client_id TEXT NOT NULL,
    professional_id TEXT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    CONSTRAINT fk_review_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_review_client FOREIGN KEY (client_id) REFERENCES users(id),
    CONSTRAINT fk_review_professional FOREIGN KEY (professional_id) REFERENCES professional_profiles(id)
);