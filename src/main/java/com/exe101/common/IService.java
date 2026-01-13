package com.exe101.common;

import java.util.List;

public interface IService<E, D, ID> {

    List<D> getAll();

    D getById(ID id);

    D create(D dto);

    D update(ID id, D dto);

    void delete(ID id);
}